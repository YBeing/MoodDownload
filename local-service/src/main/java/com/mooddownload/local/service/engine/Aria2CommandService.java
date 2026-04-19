package com.mooddownload.local.service.engine;

import com.mooddownload.local.client.aria2.Aria2RpcClient;
import com.mooddownload.local.client.aria2.dto.Aria2TaskFileDTO;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.service.engine.convert.Aria2TaskStatusConverter;
import com.mooddownload.local.service.engine.model.EngineDispatchResult;
import com.mooddownload.local.service.engine.model.EngineTaskSnapshot;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.state.TaskSourceType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * aria2 命令服务，负责提交下载和查询单任务状态。
 */
@Service
public class Aria2CommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(Aria2CommandService.class);

    private final Aria2RpcClient aria2RpcClient;

    private final Aria2TaskStatusConverter aria2TaskStatusConverter;

    public Aria2CommandService(
        Aria2RpcClient aria2RpcClient,
        Aria2TaskStatusConverter aria2TaskStatusConverter
    ) {
        this.aria2RpcClient = aria2RpcClient;
        this.aria2TaskStatusConverter = aria2TaskStatusConverter;
    }

    /**
     * 根据任务类型提交到 aria2。
     *
     * @param downloadTaskModel 下载任务模型
     * @return 分发结果
     */
    public EngineDispatchResult createDownload(DownloadTaskModel downloadTaskModel) {
        if (downloadTaskModel == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "下载任务不能为空");
        }
        TaskSourceType taskSourceType = TaskSourceType.fromCode(downloadTaskModel.getSourceType());
        if (taskSourceType == null) {
            throw new BizException(
                ErrorCode.COMMON_PARAM_INVALID,
                "不支持的下载来源类型: " + downloadTaskModel.getSourceType()
            );
        }
        String engineGid;
        if (taskSourceType == TaskSourceType.TORRENT) {
            engineGid = aria2RpcClient.addTorrent(
                downloadTaskModel.getTorrentFilePath(),
                downloadTaskModel.getSaveDir(),
                resolveOutFile(downloadTaskModel)
            );
        } else {
            engineGid = aria2RpcClient.addUri(
                downloadTaskModel.getSourceUri(),
                downloadTaskModel.getSaveDir(),
                resolveOutFile(downloadTaskModel)
            );
        }
        LOGGER.info("提交下载到 aria2 成功: taskId={}, taskCode={}, gid={}",
            downloadTaskModel.getId(), downloadTaskModel.getTaskCode(), engineGid);
        return new EngineDispatchResult(engineGid, "active", System.currentTimeMillis());
    }

    /**
     * 查询单个任务的引擎状态。
     *
     * @param engineGid aria2 gid
     * @return 引擎快照
     */
    public EngineTaskSnapshot queryTaskStatus(String engineGid) {
        EngineTaskSnapshot engineTaskSnapshot = aria2TaskStatusConverter.toSnapshot(
            aria2RpcClient.tellStatus(engineGid)
        );
        LOGGER.info("查询 aria2 任务状态成功: gid={}, engineStatus={}",
            engineTaskSnapshot.getEngineGid(), engineTaskSnapshot.getEngineStatus());
        return engineTaskSnapshot;
    }

    /**
     * 读取 BT 任务内文件列表。
     *
     * @param engineGid aria2 gid
     * @return aria2 文件列表
     */
    public List<Aria2TaskFileDTO> getFiles(String engineGid) {
        if (!StringUtils.hasText(engineGid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "engineGid 不能为空");
        }
        List<Aria2TaskFileDTO> taskFileDTOList = aria2RpcClient.getFiles(engineGid.trim());
        LOGGER.info("读取 aria2 文件列表成功: gid={}, size={}", engineGid, taskFileDTOList.size());
        return taskFileDTOList;
    }

    /**
     * 从 aria2 中移除任务，避免后端标记删除后引擎仍持续下载。
     *
     * @param engineGid aria2 gid
     * @return 被移除的 gid
     */
    public String removeDownload(String engineGid) {
        if (!StringUtils.hasText(engineGid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "engineGid 不能为空");
        }
        String normalizedEngineGid = engineGid.trim();

        try {
            String removedGid = aria2RpcClient.remove(normalizedEngineGid);
            LOGGER.info("移除 aria2 任务成功: gid={}, mode=remove", removedGid);
            return removedGid;
        } catch (BizException exception) {
            if (isTransportFailure(exception)) {
                throw exception;
            }
            LOGGER.warn("普通移除 aria2 任务失败，准备尝试强制移除: gid={}", normalizedEngineGid, exception);
        }

        try {
            String removedGid = aria2RpcClient.forceRemove(normalizedEngineGid);
            LOGGER.info("移除 aria2 任务成功: gid={}, mode=forceRemove", removedGid);
            return removedGid;
        } catch (BizException exception) {
            if (isTransportFailure(exception)) {
                throw exception;
            }
            LOGGER.warn("强制移除 aria2 任务失败，准备尝试移除结果集: gid={}", normalizedEngineGid, exception);
        }

        try {
            aria2RpcClient.removeDownloadResult(normalizedEngineGid);
            LOGGER.info("移除 aria2 下载结果成功: gid={}", normalizedEngineGid);
            return normalizedEngineGid;
        } catch (BizException exception) {
            if (isNotFound(exception)) {
                LOGGER.info("aria2 任务已不存在，按删除成功处理: gid={}", normalizedEngineGid);
                return normalizedEngineGid;
            }
            throw exception;
        }
    }

    private String resolveOutFile(DownloadTaskModel downloadTaskModel) {
        return StringUtils.hasText(downloadTaskModel.getDisplayName())
            ? downloadTaskModel.getDisplayName()
            : downloadTaskModel.getTaskCode();
    }

    private boolean isTransportFailure(BizException exception) {
        return exception != null
            && ErrorCode.EXTERNAL_ENGINE_ERROR == exception.getErrorCode()
            && "aria2 RPC 调用异常".equals(exception.getMessage());
    }

    private boolean isNotFound(BizException exception) {
        return exception != null
            && exception.getMessage() != null
            && exception.getMessage().toLowerCase().contains("not found");
    }
}
