package com.mooddownload.local.dal.task;

import com.mooddownload.local.mapper.task.TaskDeletionLogDO;
import com.mooddownload.local.mapper.task.TaskDeletionLogMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 任务删除审计 Repository。
 */
@Repository
public class TaskDeletionLogRepository {

    private final TaskDeletionLogMapper taskDeletionLogMapper;

    public TaskDeletionLogRepository(TaskDeletionLogMapper taskDeletionLogMapper) {
        this.taskDeletionLogMapper = taskDeletionLogMapper;
    }

    public void insert(TaskDeletionLogDO taskDeletionLogDO) {
        taskDeletionLogMapper.insert(taskDeletionLogDO);
    }

    public List<TaskDeletionLogDO> findByTaskId(Long taskId) {
        return taskDeletionLogMapper.selectByTaskId(taskId);
    }
}
