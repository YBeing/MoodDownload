package com.mooddownload.local.dal.task;

import com.mooddownload.local.mapper.task.DownloadEngineTaskDO;
import com.mooddownload.local.mapper.task.DownloadEngineTaskMapper;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * 下载子任务 Repository。
 */
@Repository
public class DownloadEngineTaskRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadEngineTaskRepository.class);

    private final DownloadEngineTaskMapper downloadEngineTaskMapper;

    public DownloadEngineTaskRepository(DownloadEngineTaskMapper downloadEngineTaskMapper) {
        this.downloadEngineTaskMapper = downloadEngineTaskMapper;
    }

    /**
     * 按任务 ID 查询下载子任务列表。
     *
     * @param taskId 业务任务 ID
     * @return 子任务列表
     */
    public List<DownloadEngineTaskDO> listByTaskId(Long taskId) {
        List<DownloadEngineTaskDO> downloadEngineTaskDOList = downloadEngineTaskMapper.selectByTaskId(taskId);
        if (downloadEngineTaskDOList == null || downloadEngineTaskDOList.isEmpty()) {
            return Collections.emptyList();
        }
        return downloadEngineTaskDOList.stream().collect(Collectors.toList());
    }

    /**
     * 按引擎 gid 查询下载子任务。
     *
     * @param engineGid aria2 gid
     * @return 子任务持久化对象
     */
    public Optional<DownloadEngineTaskDO> findByEngineGid(String engineGid) {
        return Optional.ofNullable(downloadEngineTaskMapper.selectByEngineGid(engineGid));
    }

    /**
     * 用最新快照替换任务下的全部子任务记录。
     *
     * @param taskId 业务任务 ID
     * @param downloadEngineTaskDOList 子任务快照列表
     */
    public void replaceByTaskId(Long taskId, List<DownloadEngineTaskDO> downloadEngineTaskDOList) {
        downloadEngineTaskMapper.deleteByTaskId(taskId);
        if (downloadEngineTaskDOList == null || downloadEngineTaskDOList.isEmpty()) {
            LOGGER.info("清空下载子任务快照成功: taskId={}", taskId);
            return;
        }
        for (DownloadEngineTaskDO downloadEngineTaskDO : downloadEngineTaskDOList) {
            int affectedRows = downloadEngineTaskMapper.insert(downloadEngineTaskDO);
            if (affectedRows != 1 || downloadEngineTaskDO.getId() == null) {
                LOGGER.error("写入下载子任务快照失败: taskId={}, engineGid={}",
                    taskId, downloadEngineTaskDO.getEngineGid());
                throw new IllegalStateException("写入下载子任务快照失败");
            }
        }
        LOGGER.info("替换下载子任务快照成功: taskId={}, size={}", taskId, downloadEngineTaskDOList.size());
    }
}
