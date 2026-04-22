package com.mooddownload.local.service.config.model;

/**
 * 更新站点规则命令。
 */
public class UpdateSourceSiteRuleCommand {

    /** 规则 ID */
    private Long ruleId;

    /** Host 匹配规则 */
    private String hostPattern;

    /** 配置模板编码 */
    private String profileCode;

    /** Tracker 集编码 */
    private String trackerSetCode;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
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
