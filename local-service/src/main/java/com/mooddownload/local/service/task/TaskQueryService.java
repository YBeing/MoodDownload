package com.mooddownload.local.service.task;

import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.dal.task.DownloadEngineTaskRepository;
import com.mooddownload.local.dal.task.DownloadTaskRepository;
import com.mooddownload.local.mapper.task.DownloadEngineTaskDO;
import com.mooddownload.local.service.task.convert.TaskModelConverter;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.model.TaskPageQuery;
import com.mooddownload.local.service.task.model.TaskPageResult;
import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import com.mooddownload.local.service.task.convert.DownloadEngineTaskModelConverter;
import java.util.Collections;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 任务查询服务，统一封装任务读取语义。
 */
@Service
public class TaskQueryService {

    private final DownloadTaskRepository downloadTaskRepository;

    private final DownloadEngineTaskRepository downloadEngineTaskRepository;

    private final TaskModelConverter taskModelConverter;

    private final DownloadEngineTaskModelConverter downloadEngineTaskModelConverter;

    public TaskQueryService(
        DownloadTaskRepository downloadTaskRepository,
        DownloadEngineTaskRepository downloadEngineTaskRepository,
        TaskModelConverter taskModelConverter,
        DownloadEngineTaskModelConverter downloadEngineTaskModelConverter
    ) {
        this.downloadTaskRepository = downloadTaskRepository;
        this.downloadEngineTaskRepository = downloadEngineTaskRepository;
        this.taskModelConverter = taskModelConverter;
        this.downloadEngineTaskModelConverter = downloadEngineTaskModelConverter;
    }

    /**
     * 按任务 ID 查询任务。
     *
     * @param taskId 任务 ID
     * @return 任务模型
     */
    public DownloadTaskModel getTaskById(Long taskId) {
        return downloadTaskRepository.findById(taskId)
            .map(this::toAggregateModel)
            .orElseThrow(() -> new BizException(ErrorCode.TASK_NOT_FOUND, "任务不存在: " + taskId));
    }

    /**
     * 按幂等键查询任务。
     *
     * @param clientRequestId 幂等键
     * @return 任务模型
     */
    public DownloadTaskModel getTaskByClientRequestId(String clientRequestId) {
        if (!StringUtils.hasText(clientRequestId)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "clientRequestId 不能为空");
        }
        return downloadTaskRepository.findByClientRequestId(clientRequestId)
            .map(this::toAggregateModel)
            .orElseThrow(() -> new BizException(ErrorCode.TASK_NOT_FOUND, "任务不存在: " + clientRequestId));
    }

    /**
     * 按 aria2 gid 查询任务。
     *
     * @param engineGid aria2 gid
     * @return 任务模型
     */
    public DownloadTaskModel getTaskByEngineGid(String engineGid) {
        if (!StringUtils.hasText(engineGid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "engineGid 不能为空");
        }
        return findTaskOptionalByEngineGid(engineGid.trim())
            .orElseThrow(() -> new BizException(ErrorCode.TASK_NOT_FOUND, "任务不存在: " + engineGid));
    }

    /**
     * 按 aria2 gid 查询任务，不存在时返回 null，供轮询同步场景降级处理。
     *
     * @param engineGid aria2 gid
     * @return 任务模型，不存在返回 null
     */
    public DownloadTaskModel findTaskByEngineGid(String engineGid) {
        if (!StringUtils.hasText(engineGid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "engineGid 不能为空");
        }
        return findTaskOptionalByEngineGid(engineGid.trim()).orElse(null);
    }

    /**
     * 查询当前可调度的待处理任务。
     *
     * @param limit 最大条数
     * @return 任务列表
     */
    public List<DownloadTaskModel> listDispatchableTasks(int limit) {
        if (limit <= 0) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "limit 必须大于 0");
        }
        return downloadTaskRepository.listByDomainStatus(DownloadTaskStatus.PENDING.name(), limit).stream()
            .map(taskModelConverter::toModel)
            .collect(Collectors.toList());
    }

    /**
     * 分页查询任务列表。
     *
     * @param taskPageQuery 分页查询条件
     * @return 分页结果
     */
    public TaskPageResult listTasks(TaskPageQuery taskPageQuery) {
        if (taskPageQuery == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "任务查询条件不能为空");
        }
        int pageNo = taskPageQuery.getPageNo() == null ? 1 : taskPageQuery.getPageNo();
        int pageSize = taskPageQuery.getPageSize() == null ? 50 : taskPageQuery.getPageSize();
        if (pageNo <= 0 || pageSize <= 0) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "pageNo 和 pageSize 必须大于 0");
        }

        String normalizedStatus = normalizeStatus(taskPageQuery.getStatus());
        String normalizedKeyword = normalizeKeyword(taskPageQuery.getKeyword());
        int offset = (pageNo - 1) * pageSize;

        TaskPageResult taskPageResult = new TaskPageResult();
        taskPageResult.setPageNo(pageNo);
        taskPageResult.setPageSize(pageSize);
        taskPageResult.setTotal(downloadTaskRepository.countByCondition(normalizedStatus, normalizedKeyword));
        taskPageResult.setItems(downloadTaskRepository.searchPage(normalizedStatus, normalizedKeyword, offset, pageSize)
            .stream()
            .map(taskModelConverter::toModel)
            .collect(Collectors.toList()));
        return taskPageResult;
    }

    /**
     * 查询全部未删除任务，并补齐子任务明细，供轮询聚合同步使用。
     *
     * @return 全量任务列表
     */
    public List<DownloadTaskModel> listAllActiveTasks() {
        return downloadTaskRepository.listAllActiveTasks().stream()
            .map(this::toAggregateModel)
            .collect(Collectors.toList());
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return DownloadTaskStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "不支持的任务状态: " + status);
        }
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private Optional<DownloadTaskModel> findTaskOptionalByEngineGid(String engineGid) {
        Optional<DownloadTaskModel> directTaskOptional = downloadTaskRepository.findByEngineGid(engineGid)
            .map(this::toAggregateModel);
        if (directTaskOptional.isPresent()) {
            return directTaskOptional;
        }
        Optional<DownloadEngineTaskDO> downloadEngineTaskOptional = downloadEngineTaskRepository.findByEngineGid(engineGid);
        if (!downloadEngineTaskOptional.isPresent() || downloadEngineTaskOptional.get().getTaskId() == null) {
            return Optional.empty();
        }
        return downloadTaskRepository.findById(downloadEngineTaskOptional.get().getTaskId())
            .map(this::toAggregateModel);
    }

    private DownloadTaskModel toAggregateModel(com.mooddownload.local.mapper.task.DownloadTaskDO downloadTaskDO) {
        DownloadTaskModel downloadTaskModel = taskModelConverter.toModel(downloadTaskDO);
        if (downloadTaskModel == null || downloadTaskModel.getId() == null) {
            return downloadTaskModel;
        }
        downloadTaskModel.setEngineTasks(downloadEngineTaskRepository.listByTaskId(downloadTaskModel.getId()).stream()
            .map(downloadEngineTaskModelConverter::toModel)
            .collect(Collectors.toList()));
        if (downloadTaskModel.getEngineTasks() == null) {
            downloadTaskModel.setEngineTasks(Collections.emptyList());
        }
        return downloadTaskModel;
    }
}
