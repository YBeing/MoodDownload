package com.mooddownload.local.mapper.profile;

/**
 * 站点规则持久化对象。
 */
public class SourceSiteRuleDO {

    /** 主键 ID */
    private Long id;

    /** Host 匹配规则 */
    private String hostPattern;

    /** 来源类型 */
    private String sourceType;

    /** 浏览器编码 */
    private String browserCode;

    /** 引擎配置模板编码 */
    private String profileCode;

    /** Tracker 集编码 */
    private String trackerSetCode;

    /** 是否要求请求头快照 */
    private Integer requireHeaderSnapshot;

    /** 是否启用 */
    private Integer enabled;

    /** 优先级 */
    private Integer priority;

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

    public String getHostPattern() {
        return hostPattern;
    }

    public void setHostPattern(String hostPattern) {
        this.hostPattern = hostPattern;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getBrowserCode() {
        return browserCode;
    }

    public void setBrowserCode(String browserCode) {
        this.browserCode = browserCode;
    }

    public String getProfileCode() {
        return profileCode;
    }

    public void setProfileCode(String profileCode) {
        this.profileCode = profileCode;
    }

    public String getTrackerSetCode() {
        return trackerSetCode;
    }

    public void setTrackerSetCode(String trackerSetCode) {
        this.trackerSetCode = trackerSetCode;
    }

    public Integer getRequireHeaderSnapshot() {
        return requireHeaderSnapshot;
    }

    public void setRequireHeaderSnapshot(Integer requireHeaderSnapshot) {
        this.requireHeaderSnapshot = requireHeaderSnapshot;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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
