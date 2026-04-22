package com.mooddownload.local.service.config.model;

import java.util.List;

/**
 * 引擎运行配置中心快照模型。
 */
public class EngineRuntimeSnapshotModel {

    /** 当前激活的配置模板编码 */
    private String activeProfileCode;

    /** 模板列表 */
    private List<EngineRuntimeProfileItemModel> profiles;

    /** 应用状态 */
    private String applyStatus;

    public String getActiveProfileCode() {
        return activeProfileCode;
    }

    public void setActiveProfileCode(String activeProfileCode) {
        this.activeProfileCode = activeProfileCode;
    }

    public List<EngineRuntimeProfileItemModel> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<EngineRuntimeProfileItemModel> profiles) {
        this.profiles = profiles;
    }

    public String getApplyStatus() {
        return applyStatus;
    }

    public void setApplyStatus(String applyStatus) {
        this.applyStatus = applyStatus;
    }
}
