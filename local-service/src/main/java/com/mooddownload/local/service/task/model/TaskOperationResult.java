package com.mooddownload.local.service.task.model;

import com.mooddownload.local.service.task.event.TaskDomainEvent;

/**
 * 任务命令执行结果。
 */
public class TaskOperationResult {

    /** 当前任务快照 */
    private final DownloadTaskModel taskModel;

    /** 本次产生的领域事件 */
    private final TaskDomainEvent taskDomainEvent;

    /** 是否命中幂等返回 */
    private final boolean idempotent;

    public TaskOperationResult(
        DownloadTaskModel taskModel,
        TaskDomainEvent taskDomainEvent,
        boolean idempotent
    ) {
        this.taskModel = taskModel;
        this.taskDomainEvent = taskDomainEvent;
        this.idempotent = idempotent;
    }

    public DownloadTaskModel getTaskModel() {
        return taskModel;
    }

    public TaskDomainEvent getTaskDomainEvent() {
        return taskDomainEvent;
    }

    public boolean isIdempotent() {
        return idempotent;
    }
}
