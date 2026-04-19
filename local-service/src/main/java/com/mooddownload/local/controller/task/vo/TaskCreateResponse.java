package com.mooddownload.local.controller.task.vo;

/**
 * 创建任务响应。
 */
public class TaskCreateResponse {

    /** 任务 ID */
    private Long taskId;

    /** 任务编码 */
    private String taskCode;

    /** 领域状态 */
    private String domainStatus;

    /** 引擎状态 */
    private String engineStatus;

    /** 展示名称 */
    private String displayName;

    /** 创建时间 */
    private Long createdAt;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getDomainStatus() {
        return domainStatus;
    }

    public void setDomainStatus(String domainStatus) {
        this.domainStatus = domainStatus;
    }

    public String getEngineStatus() {
        return engineStatus;
    }

    public void setEngineStatus(String engineStatus) {
        this.engineStatus = engineStatus;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
