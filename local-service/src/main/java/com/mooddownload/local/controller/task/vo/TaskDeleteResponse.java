package com.mooddownload.local.controller.task.vo;

/**
 * 删除任务响应。
 */
public class TaskDeleteResponse {

    /** 任务 ID */
    private Long taskId;

    /** 是否已删除 */
    private Boolean removed;

    /** 文件是否已删除 */
    private Boolean filesRemoved;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Boolean getRemoved() {
        return removed;
    }

    public void setRemoved(Boolean removed) {
        this.removed = removed;
    }

    public Boolean getFilesRemoved() {
        return filesRemoved;
    }

    public void setFilesRemoved(Boolean filesRemoved) {
        this.filesRemoved = filesRemoved;
    }
}
