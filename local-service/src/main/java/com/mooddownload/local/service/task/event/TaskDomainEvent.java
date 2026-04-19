package com.mooddownload.local.service.task.event;

import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import com.mooddownload.local.service.task.state.TaskTriggerType;

/**
 * 任务领域事件。
 */
public class TaskDomainEvent {

    /** 任务 ID */
    private final Long taskId;

    /** 业务任务编码 */
    private final String taskCode;

    /** 变更前状态 */
    private final DownloadTaskStatus fromStatus;

    /** 变更后状态 */
    private final DownloadTaskStatus toStatus;

    /** 状态变更触发器 */
    private final TaskTriggerType triggerType;

    /** 事件时间 */
    private final Long occurredAt;

    public TaskDomainEvent(
        Long taskId,
        String taskCode,
        DownloadTaskStatus fromStatus,
        DownloadTaskStatus toStatus,
        TaskTriggerType triggerType,
        Long occurredAt
    ) {
        this.taskId = taskId;
        this.taskCode = taskCode;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.triggerType = triggerType;
        this.occurredAt = occurredAt;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public DownloadTaskStatus getFromStatus() {
        return fromStatus;
    }

    public DownloadTaskStatus getToStatus() {
        return toStatus;
    }

    public TaskTriggerType getTriggerType() {
        return triggerType;
    }

    public Long getOccurredAt() {
        return occurredAt;
    }
}
