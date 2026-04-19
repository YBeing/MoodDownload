package com.mooddownload.local.dal.task;

import com.mooddownload.local.mapper.task.DownloadAttemptDO;
import com.mooddownload.local.mapper.task.DownloadAttemptMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * 下载尝试 Repository。
 */
@Repository
public class DownloadAttemptRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadAttemptRepository.class);

    private final DownloadAttemptMapper downloadAttemptMapper;

    public DownloadAttemptRepository(DownloadAttemptMapper downloadAttemptMapper) {
        this.downloadAttemptMapper = downloadAttemptMapper;
    }

    /**
     * 保存下载尝试记录。
     *
     * @param downloadAttemptDO 下载尝试持久化对象
     * @return 尝试记录主键
     */
    public Long save(DownloadAttemptDO downloadAttemptDO) {
        int affectedRows = downloadAttemptMapper.insert(downloadAttemptDO);
        if (affectedRows != 1 || downloadAttemptDO.getId() == null) {
            LOGGER.error("写入下载尝试失败: taskId={}, attemptNo={}",
                downloadAttemptDO.getTaskId(), downloadAttemptDO.getAttemptNo());
            throw new IllegalStateException("写入下载尝试失败");
        }
        LOGGER.info("写入下载尝试成功: attemptId={}, taskId={}, attemptNo={}",
            downloadAttemptDO.getId(), downloadAttemptDO.getTaskId(), downloadAttemptDO.getAttemptNo());
        return downloadAttemptDO.getId();
    }

    /**
     * 查询任务关联的尝试记录。
     *
     * @param taskId 任务 ID
     * @return 尝试记录列表
     */
    public List<DownloadAttemptDO> listByTaskId(Long taskId) {
        return downloadAttemptMapper.selectByTaskId(taskId);
    }
}
