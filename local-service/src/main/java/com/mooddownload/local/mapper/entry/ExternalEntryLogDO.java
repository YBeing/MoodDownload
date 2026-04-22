package com.mooddownload.local.mapper.entry;

/**
 * 外部入口审计持久化对象。
 */
public class ExternalEntryLogDO {

    /** 主键 ID */
    private Long id;

    /** 幂等请求键 */
    private String clientRequestId;

    /** 入口类型 */
    private String entryType;

    /** 浏览器编码 */
    private String browserCode;

    /** 来源类型 */
    private String sourceType;

    /** 页面地址 */
    private String tabUrl;

    /** 来源地址 */
    private String sourceUri;

    /** 命中的规则 ID */
    private Long matchedRuleId;

    /** 执行结果状态 */
    private String resultStatus;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Long createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public String getBrowserCode() {
        return browserCode;
    }

    public void setBrowserCode(String browserCode) {
        this.browserCode = browserCode;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTabUrl() {
        return tabUrl;
    }

    public void setTabUrl(String tabUrl) {
        this.tabUrl = tabUrl;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public Long getMatchedRuleId() {
        return matchedRuleId;
    }

    public void setMatchedRuleId(Long matchedRuleId) {
        this.matchedRuleId = matchedRuleId;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
