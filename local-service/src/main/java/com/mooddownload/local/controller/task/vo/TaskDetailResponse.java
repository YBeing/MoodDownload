package com.mooddownload.local.controller.task.vo;

import java.util.List;

/**
 * 任务详情响应。
 */
public class TaskDetailResponse {

    /** 任务 ID */
    private Long taskId;

    /** 任务编码 */
    private String taskCode;

    /** 展示名称 */
    private String displayName;

    /** 来源类型 */
    private String sourceType;

    /** 来源地址 */
    private String sourceUri;

    /** 领域状态 */
    private String domainStatus;

    /** 引擎状态 */
    private String engineStatus;

    /** 进度 */
    private Double progress;

    /** 总大小 */
    private Long totalSizeBytes;

    /** 已完成大小 */
    private Long completedSizeBytes;

    /** 下载速度 */
    private Long downloadSpeedBps;

    /** 上传速度 */
    private Long uploadSpeedBps;

    /** 保存目录 */
    private String saveDir;

    /** 重试次数 */
    private Integer retryCount;

    /** 错误码 */
    private String errorCode;

    /** 错误信息 */
    private String errorMessage;

    /** BT 文件列表 */
    private List<TaskTorrentFileVO> torrentFiles;

    /** aria2 子任务列表 */
    private List<TaskEngineDetailVO> engineTasks;

    /** BT 元数据是否已解析完成 */
    private Boolean torrentMetadataReady;

    /** 创建时间 */
    private Long createdAt;

    /** 更新时间 */
    private Long updatedAt;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public String getDomainStatus() {
        return domainStatus;
    }

    public void setDomainStatus(String domainStatus) {
        this.domainStatus = domainStatus;
    }

    public String getEngineStatus() {
        return engineStatus;
    }

    public void setEngineStatus(String engineStatus) {
        this.engineStatus = engineStatus;
    }

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
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

    public String getSaveDir() {
        return saveDir;
    }

    public void setSaveDir(String saveDir) {
        this.saveDir = saveDir;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
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

    public List<TaskTorrentFileVO> getTorrentFiles() {
        return torrentFiles;
    }

    public void setTorrentFiles(List<TaskTorrentFileVO> torrentFiles) {
        this.torrentFiles = torrentFiles;
    }

    public List<TaskEngineDetailVO> getEngineTasks() {
        return engineTasks;
    }

    public void setEngineTasks(List<TaskEngineDetailVO> engineTasks) {
        this.engineTasks = engineTasks;
    }

    public Boolean getTorrentMetadataReady() {
        return torrentMetadataReady;
    }

    public void setTorrentMetadataReady(Boolean torrentMetadataReady) {
        this.torrentMetadataReady = torrentMetadataReady;
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
