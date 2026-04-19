package com.mooddownload.local.controller.task.vo;

/**
 * 任务操作响应。
 */
public class TaskOperationResponse {

    /** 任务 ID */
    private Long taskId;

    /** 领域状态 */
    private String domainStatus;

    /** 当前重试次数 */
    private Integer retryCount;

    /** 是否实际执行 */
    private Boolean operationApplied;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getDomainStatus() {
        return domainStatus;
    }

    public void setDomainStatus(String domainStatus) {
        this.domainStatus = domainStatus;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Boolean getOperationApplied() {
        return operationApplied;
    }

    public void setOperationApplied(Boolean operationApplied) {
        this.operationApplied = operationApplied;
    }
}
