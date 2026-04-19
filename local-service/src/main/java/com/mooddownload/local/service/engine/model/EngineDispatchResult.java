package com.mooddownload.local.service.engine.model;

/**
 * 引擎分发结果。
 */
public class EngineDispatchResult {

    /** aria2 gid */
    private final String engineGid;

    /** 引擎状态 */
    private final String engineStatus;

    /** 分发时间 */
    private final Long dispatchedAt;

    public EngineDispatchResult(String engineGid, String engineStatus, Long dispatchedAt) {
        this.engineGid = engineGid;
        this.engineStatus = engineStatus;
        this.dispatchedAt = dispatchedAt;
    }

    public String getEngineGid() {
        return engineGid;
    }

    public String getEngineStatus() {
        return engineStatus;
    }

    public Long getDispatchedAt() {
        return dispatchedAt;
    }
}
