package com.mooddownload.local.controller.config.vo;

import javax.validation.constraints.NotBlank;

/**
 * 更新引擎运行配置请求。
 */
public class UpdateEngineRuntimeProfileRequest {

    /** 配置模板编码 */
    @NotBlank(message = "不能为空")
    private String profileCode;

    /** 配置模板 JSON */
    @NotBlank(message = "不能为空")
    private String profileJson;

    public String getProfileCode() {
        return profileCode;
    }

    public void setProfileCode(String profileCode) {
        this.profileCode = profileCode;
    }

    public String getProfileJson() {
        return profileJson;
    }

    public void setProfileJson(String profileJson) {
        this.profileJson = profileJson;
    }
}
