package com.mooddownload.local.service.task.model;

import java.util.List;

/**
 * 下载任务领域模型。
 */
public class DownloadTaskModel {

    /** 主键 ID */
    private Long id;

    /** 业务任务编码 */
    private String taskCode;

    /** 来源类型 */
    private String sourceType;

    /** 来源地址 */
    private String sourceUri;

    /** 来源哈希 */
    private String sourceHash;

    /** 种子文件路径 */
    private String torrentFilePath;

    /** BT 文件列表序列化结果 */
    private String torrentFileListJson;

    /** BT 文件列表 */
    private List<TorrentFileItem> torrentFiles;

    /** aria2 子任务列表 */
    private List<DownloadEngineTaskModel> engineTasks;

    /** 展示名称 */
    private String displayName;

    /** 领域状态 */
    private String domainStatus;

    /** 引擎状态 */
    private String engineStatus;

    /** 引擎 gid */
    private String engineGid;

    /** 队列优先级 */
    private Integer queuePriority;

    /** 保存目录 */
    private String saveDir;

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

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetryCount;

    /** 幂等请求键 */
    private String clientRequestId;

    /** 最近同步时间 */
    private Long lastSyncAt;

    /** 乐观锁版本 */
    private Integer version;

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

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
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

    public String getSourceHash() {
        return sourceHash;
    }

    public void setSourceHash(String sourceHash) {
        this.sourceHash = sourceHash;
    }

    public String getTorrentFilePath() {
        return torrentFilePath;
    }

    public void setTorrentFilePath(String torrentFilePath) {
        this.torrentFilePath = torrentFilePath;
    }

    public String getTorrentFileListJson() {
        return torrentFileListJson;
    }

    public void setTorrentFileListJson(String torrentFileListJson) {
        this.torrentFileListJson = torrentFileListJson;
    }

    public List<TorrentFileItem> getTorrentFiles() {
        return torrentFiles;
    }

    public void setTorrentFiles(List<TorrentFileItem> torrentFiles) {
        this.torrentFiles = torrentFiles;
    }

    public List<DownloadEngineTaskModel> getEngineTasks() {
        return engineTasks;
    }

    public void setEngineTasks(List<DownloadEngineTaskModel> engineTasks) {
        this.engineTasks = engineTasks;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public String getEngineGid() {
        return engineGid;
    }

    public void setEngineGid(String engineGid) {
        this.engineGid = engineGid;
    }

    public Integer getQueuePriority() {
        return queuePriority;
    }

    public void setQueuePriority(Integer queuePriority) {
        this.queuePriority = queuePriority;
    }

    public String getSaveDir() {
        return saveDir;
    }

    public void setSaveDir(String saveDir) {
        this.saveDir = saveDir;
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

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    public Long getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Long lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
