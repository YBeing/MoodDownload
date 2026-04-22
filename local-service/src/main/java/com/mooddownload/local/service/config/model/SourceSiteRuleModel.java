package com.mooddownload.local.service.config.model;

/**
 * 站点规则模型。
 */
public class SourceSiteRuleModel {

    /** 主键 ID */
    private Long id;

    /** Host 匹配规则 */
    private String hostPattern;

    /** 配置模板编码 */
    private String profileCode;

    /** Tracker 集编码 */
    private String trackerSetCode;

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
}
