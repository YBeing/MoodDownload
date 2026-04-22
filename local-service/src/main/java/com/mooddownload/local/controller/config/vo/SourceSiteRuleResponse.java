package com.mooddownload.local.controller.config.vo;

/**
 * 站点规则响应。
 */
public class SourceSiteRuleResponse {

    /** 主键 ID */
    private Long id;

    /** Host 匹配规则 */
    private String hostPattern;

    /** 引擎配置模板编码 */
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
