package com.mooddownload.local.controller.task.vo;

/**
 * 任务详情中的 aria2 子任务视图对象。
 */
public class TaskEngineDetailVO {

    /** aria2 子任务 gid */
    private String engineGid;

    /** 父级 aria2 子任务 gid */
    private String parentEngineGid;

    /** 引擎状态 */
    private String engineStatus;

    /** 是否仅为 metadata 子任务 */
    private Boolean metadataOnly;

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

    /** 错误信息 */
    private String errorMessage;

    public String getEngineGid() {
        return engineGid;
    }

    public void setEngineGid(String engineGid) {
        this.engineGid = engineGid;
    }

    public String getParentEngineGid() {
        return parentEngineGid;
    }

    public void setParentEngineGid(String parentEngineGid) {
        this.parentEngineGid = parentEngineGid;
    }

    public String getEngineStatus() {
        return engineStatus;
    }

    public void setEngineStatus(String engineStatus) {
        this.engineStatus = engineStatus;
    }

    public Boolean getMetadataOnly() {
        return metadataOnly;
    }

    public void setMetadataOnly(Boolean metadataOnly) {
        this.metadataOnly = metadataOnly;
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
}
