package com.mooddownload.local.service.engine.model;

import java.util.List;

/**
 * 引擎任务快照。
 */
public class EngineTaskSnapshot {

    /** aria2 gid */
    private String engineGid;

    /** 引擎状态 */
    private String engineStatus;

    /** 总大小 */
    private Long totalSizeBytes;

    /** 已完成大小 */
    private Long completedSizeBytes;

    /** 下载速度 */
    private Long downloadSpeedBps;

    /** 上传速度 */
    private Long uploadSpeedBps;

    /** 错误码 */
    private String errorCode;

    /** 错误消息 */
    private String errorMessage;

    /** 后续承接任务 gid 列表 */
    private List<String> followedBy;

    /** 前置依赖任务 gid */
    private String belongsTo;

    public String getEngineGid() {
        return engineGid;
    }

    public void setEngineGid(String engineGid) {
        this.engineGid = engineGid;
    }

    public String getEngineStatus() {
        return engineStatus;
    }

    public void setEngineStatus(String engineStatus) {
        this.engineStatus = engineStatus;
    }

    public Long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(Long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    public Long getCompletedSizeBytes() {
        return completedSizeBytes;
    }

    public void setCompletedSizeBytes(Long completedSizeBytes) {
        this.completedSizeBytes = completedSizeBytes;
    }

    public Long getDownloadSpeedBps() {
        return downloadSpeedBps;
    }

    public void setDownloadSpeedBps(Long downloadSpeedBps) {
        this.downloadSpeedBps = downloadSpeedBps;
    }

    public Long getUploadSpeedBps() {
        return uploadSpeedBps;
    }

    public void setUploadSpeedBps(Long uploadSpeedBps) {
        this.uploadSpeedBps = uploadSpeedBps;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getFollowedBy() {
        return followedBy;
    }

    public void setFollowedBy(List<String> followedBy) {
        this.followedBy = followedBy;
    }

    public String getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(String belongsTo) {
        this.belongsTo = belongsTo;
    }
}
