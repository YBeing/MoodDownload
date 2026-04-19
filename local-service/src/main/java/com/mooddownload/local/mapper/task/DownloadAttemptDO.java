package com.mooddownload.local.mapper.task;

/**
 * 下载尝试持久化对象。
 */
public class DownloadAttemptDO {

    /** 主键 ID */
    private Long id;

    /** 任务 ID */
    private Long taskId;

    /** 第几次尝试 */
    private Integer attemptNo;

    /** 触发原因 */
    private String triggerReason;

    /** 执行结果 */
    private String resultStatus;

    /** 关联的引擎 gid */
    private String engineGid;

    /** 失败阶段 */
    private String failPhase;

    /** 失败描述 */
    private String failMessage;

    /** 开始时间 */
    private Long startedAt;

    /** 结束时间 */
    private Long finishedAt;

    /** 创建时间 */
    private Long createdAt;

    /** 更新时间 */
    private Long updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public String getTriggerReason() {
        return triggerReason;
    }

    public void setTriggerReason(String triggerReason) {
        this.triggerReason = triggerReason;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getEngineGid() {
        return engineGid;
    }

    public void setEngineGid(String engineGid) {
        this.engineGid = engineGid;
    }

    public String getFailPhase() {
        return failPhase;
    }

    public void setFailPhase(String failPhase) {
        this.failPhase = failPhase;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }

    public Long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
    }

    public Long getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Long finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
