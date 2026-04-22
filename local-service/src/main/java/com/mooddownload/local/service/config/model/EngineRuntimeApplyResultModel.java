package com.mooddownload.local.service.config.model;

/**
 * 引擎运行配置应用结果模型。
 */
public class EngineRuntimeApplyResultModel {

    /** 配置模板编码 */
    private String profileCode;

    /** 应用状态 */
    private String applyStatus;

    /** 是否需要重启引擎 */
    private Boolean restartRequired;

    public String getProfileCode() {
        return profileCode;
    }

    public void setProfileCode(String profileCode) {
        this.profileCode = profileCode;
    }

    public String getApplyStatus() {
        return applyStatus;
    }

    public void setApplyStatus(String applyStatus) {
        this.applyStatus = applyStatus;
    }

    public Boolean getRestartRequired() {
        return restartRequired;
    }

    public void setRestartRequired(Boolean restartRequired) {
        this.restartRequired = restartRequired;
    }
}
