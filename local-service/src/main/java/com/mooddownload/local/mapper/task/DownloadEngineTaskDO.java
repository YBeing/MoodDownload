package com.mooddownload.local.mapper.task;

/**
 * 下载子任务持久化对象。
 */
public class DownloadEngineTaskDO {

    /** 主键 ID */
    private Long id;

    /** 归属业务任务 ID */
    private Long taskId;

    /** aria2 子任务 gid */
    private String engineGid;

    /** 父级 aria2 子任务 gid */
    private String parentEngineGid;

    /** 引擎状态 */
    private String engineStatus;

    /** 子任务文件列表 JSON */
    private String torrentFileListJson;

    /** 是否仅为 metadata 子任务 */
    private Integer metadataOnly;

    /** 子任务总大小 */
    private Long totalSizeBytes;

    /** 子任务已完成大小 */
    private Long completedSizeBytes;

    /** 子任务下载速度 */
    private Long downloadSpeedBps;

    /** 子任务上传速度 */
    private Long uploadSpeedBps;

    /** 子任务错误码 */
    private String errorCode;

    /** 子任务错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private Long createdAt;

    /** 更新时间 */
    private Long updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

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

    public String getTorrentFileListJson() {
        return torrentFileListJson;
    }

    public void setTorrentFileListJson(String torrentFileListJson) {
        this.torrentFileListJson = torrentFileListJson;
    }

    public Integer getMetadataOnly() {
        return metadataOnly;
    }

    public void setMetadataOnly(Integer metadataOnly) {
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

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
