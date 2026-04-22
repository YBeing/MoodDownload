package com.mooddownload.local.service.config.model;

/**
 * 更新引擎配置模板命令。
 */
public class UpdateEngineRuntimeProfileCommand {

    /** 配置模板编码 */
    private String profileCode;

    /** 配置模板 JSON */
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
