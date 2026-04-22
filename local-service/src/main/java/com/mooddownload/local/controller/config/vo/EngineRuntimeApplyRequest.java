package com.mooddownload.local.controller.config.vo;

import javax.validation.constraints.NotBlank;

/**
 * 应用引擎运行配置请求。
 */
public class EngineRuntimeApplyRequest {

    /** 配置模板编码 */
    @NotBlank(message = "不能为空")
    private String profileCode;

    /** 是否强制重启应用 */
    private Boolean forceRestart;

    public String getProfileCode() {
        return profileCode;
    }

    public void setProfileCode(String profileCode) {
        this.profileCode = profileCode;
    }

    public Boolean getForceRestart() {
        return forceRestart;
    }

    public void setForceRestart(Boolean forceRestart) {
        this.forceRestart = forceRestart;
    }
}
