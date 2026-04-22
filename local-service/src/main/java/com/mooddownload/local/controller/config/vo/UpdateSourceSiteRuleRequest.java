package com.mooddownload.local.controller.config.vo;

import javax.validation.constraints.NotBlank;

/**
 * 更新站点规则请求。
 */
public class UpdateSourceSiteRuleRequest {

    /** Host 匹配规则 */
    @NotBlank(message = "不能为空")
    private String hostPattern;

    /** 引擎配置模板编码 */
    @NotBlank(message = "不能为空")
    private String profileCode;

    /** Tracker 集编码 */
    private String trackerSetCode;

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
