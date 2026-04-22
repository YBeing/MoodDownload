package com.mooddownload.local.controller.config.vo;

/**
 * 应用引擎运行配置响应。
 */
public class EngineRuntimeApplyResponse {

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
