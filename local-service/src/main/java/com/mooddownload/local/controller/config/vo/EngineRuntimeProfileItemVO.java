package com.mooddownload.local.controller.config.vo;

/**
 * 引擎配置模板条目响应。
 */
public class EngineRuntimeProfileItemVO {

    /** 配置模板编码 */
    private String profileCode;

    /** 配置模板名称 */
    private String profileName;

    /** 绑定的 Tracker 集编码 */
    private String trackerSetCode;

    /** 模板配置 JSON */
    private String profileJson;

    /** 是否默认模板 */
    private Boolean isDefault;

    /** 是否启用 */
    private Boolean enabled;

    public String getProfileCode() {
        return profileCode;
    }

    public void setProfileCode(String profileCode) {
        this.profileCode = profileCode;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getTrackerSetCode() {
        return trackerSetCode;
    }

    public void setTrackerSetCode(String trackerSetCode) {
        this.trackerSetCode = trackerSetCode;
    }

    public String getProfileJson() {
        return profileJson;
    }

    public void setProfileJson(String profileJson) {
        this.profileJson = profileJson;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
