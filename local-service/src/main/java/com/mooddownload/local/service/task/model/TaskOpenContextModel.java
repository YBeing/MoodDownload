package com.mooddownload.local.service.task.model;

/**
 * 打开文件夹上下文模型。
 */
public class TaskOpenContextModel {

    /** 任务 ID */
    private Long taskId;

    /** 打开目录 */
    private String openFolderPath;

    /** 主文件路径 */
    private String primaryFilePath;

    /** 是否可打开 */
    private Boolean canOpen;

    /** 原因说明 */
    private String reason;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getOpenFolderPath() {
        return openFolderPath;
    }

    public void setOpenFolderPath(String openFolderPath) {
        this.openFolderPath = openFolderPath;
    }

    public String getPrimaryFilePath() {
        return primaryFilePath;
    }

    public void setPrimaryFilePath(String primaryFilePath) {
        this.primaryFilePath = primaryFilePath;
    }

    public Boolean getCanOpen() {
        return canOpen;
    }

    public void setCanOpen(Boolean canOpen) {
        this.canOpen = canOpen;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
