package com.mooddownload.local.mapper.profile;

/**
 * 引擎运行配置模板持久化对象。
 */
public class EngineRuntimeProfileDO {

    /** 配置模板编码 */
    private String profileCode;

    /** 配置模板名称 */
    private String profileName;

    /** 绑定的 Tracker 集编码 */
    private String trackerSetCode;

    /** 生效范围 */
    private String applyScope;

    /** 引擎配置 JSON */
    private String profileJson;

    /** 是否启用 */
    private Integer enabled;

    /** 是否默认模板 */
    private Integer isDefault;

    /** 版本号 */
    private Integer version;

    /** 创建时间 */
    private Long createdAt;

    /** 更新时间 */
    private Long updatedAt;

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

    public String getApplyScope() {
        return applyScope;
    }

    public void setApplyScope(String applyScope) {
        this.applyScope = applyScope;
    }

    public String getProfileJson() {
        return profileJson;
    }

    public void setProfileJson(String profileJson) {
        this.profileJson = profileJson;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
