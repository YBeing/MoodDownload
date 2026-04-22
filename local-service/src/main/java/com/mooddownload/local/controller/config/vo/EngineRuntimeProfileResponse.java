package com.mooddownload.local.controller.config.vo;

import java.util.List;

/**
 * 引擎运行配置响应。
 */
public class EngineRuntimeProfileResponse {

    /** 当前生效的配置模板编码 */
    private String activeProfileCode;

    /** 配置模板列表 */
    private List<EngineRuntimeProfileItemVO> profiles;

    /** 应用状态 */
    private String applyStatus;

    public String getActiveProfileCode() {
        return activeProfileCode;
    }

    public void setActiveProfileCode(String activeProfileCode) {
        this.activeProfileCode = activeProfileCode;
    }

    public List<EngineRuntimeProfileItemVO> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<EngineRuntimeProfileItemVO> profiles) {
        this.profiles = profiles;
    }

    public String getApplyStatus() {
        return applyStatus;
    }

    public void setApplyStatus(String applyStatus) {
        this.applyStatus = applyStatus;
    }
}
