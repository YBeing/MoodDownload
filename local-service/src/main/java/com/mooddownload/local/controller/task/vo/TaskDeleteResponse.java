package com.mooddownload.local.controller.task.vo;

/**
 * 删除任务响应。
 */
public class TaskDeleteResponse {

    /** 任务 ID */
    private Long taskId;

    /** 是否已删除 */
    private Boolean removed;

    /** 删除模式 */
    private String deleteMode;

    /** 输出文件是否已删除 */
    private Boolean outputRemoved;

    /** 关联工件是否已删除 */
    private Boolean artifactRemoved;

    /** 是否部分成功 */
    private Boolean partialSuccess;

    /** 结果说明 */
    private String message;

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

    public String getDeleteMode() {
        return deleteMode;
    }

    public void setDeleteMode(String deleteMode) {
        this.deleteMode = deleteMode;
    }

    public Boolean getOutputRemoved() {
        return outputRemoved;
    }

    public void setOutputRemoved(Boolean outputRemoved) {
        this.outputRemoved = outputRemoved;
    }

    public Boolean getArtifactRemoved() {
        return artifactRemoved;
    }

    public void setArtifactRemoved(Boolean artifactRemoved) {
        this.artifactRemoved = artifactRemoved;
    }

    public Boolean getPartialSuccess() {
        return partialSuccess;
    }

    public void setPartialSuccess(Boolean partialSuccess) {
        this.partialSuccess = partialSuccess;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
