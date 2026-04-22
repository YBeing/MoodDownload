package com.mooddownload.local.service.capture.model;

/**
 * capture 场景统一返回结果。
 */
public class CaptureTaskResult {

    /** 是否已接收入队 */
    private Boolean accepted;

    /** 任务 ID */
    private Long taskId;

    /** 任务编码 */
    private String taskCode;

    /** 当前领域状态 */
    private String domainStatus;

    /** 解析后的来源类型 */
    private String resolvedSourceType;

    /** 是否命中站点规则 */
    private Boolean siteRuleMatched;

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

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

    public String getResolvedSourceType() {
        return resolvedSourceType;
    }

    public void setResolvedSourceType(String resolvedSourceType) {
        this.resolvedSourceType = resolvedSourceType;
    }

    public Boolean getSiteRuleMatched() {
        return siteRuleMatched;
    }

    public void setSiteRuleMatched(Boolean siteRuleMatched) {
        this.siteRuleMatched = siteRuleMatched;
    }
}
