package com.mooddownload.local.dal.task;

import com.mooddownload.local.mapper.task.TaskStateLogDO;
import com.mooddownload.local.mapper.task.TaskStateLogMapper;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * 任务状态日志 Repository。
 */
@Repository
public class TaskStateLogRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStateLogRepository.class);

    private final TaskStateLogMapper taskStateLogMapper;

    public TaskStateLogRepository(TaskStateLogMapper taskStateLogMapper) {
        this.taskStateLogMapper = taskStateLogMapper;
    }

    /**
     * 保存任务状态流转日志。
     *
     * @param taskStateLogDO 状态日志持久化对象
     * @return 日志主键
     */
    public Long save(TaskStateLogDO taskStateLogDO) {
        int affectedRows = taskStateLogMapper.insert(taskStateLogDO);
        if (affectedRows != 1 || taskStateLogDO.getId() == null) {
            LOGGER.error("写入任务状态日志失败: taskId={}, toStatus={}",
                taskStateLogDO.getTaskId(), taskStateLogDO.getToStatus());
            throw new IllegalStateException("写入任务状态日志失败");
        }
        LOGGER.info("写入任务状态日志成功: logId={}, taskId={}, toStatus={}",
            taskStateLogDO.getId(), taskStateLogDO.getTaskId(), taskStateLogDO.getToStatus());
        return taskStateLogDO.getId();
    }

    /**
     * 查询任务状态日志。
     *
     * @param taskId 任务 ID
     * @return 状态日志列表
     */
    public List<TaskStateLogDO> listByTaskId(Long taskId) {
        return taskStateLogMapper.selectByTaskId(taskId);
    }

    /**
     * 查询任务最近一条状态日志。
     *
     * @param taskId 任务 ID
     * @return 最近状态日志
     */
    public Optional<TaskStateLogDO> findLatestByTaskId(Long taskId) {
        return Optional.ofNullable(taskStateLogMapper.selectLatestByTaskId(taskId));
    }
}
