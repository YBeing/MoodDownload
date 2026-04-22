package com.mooddownload.local.mapper.task;

/**
 * 下载任务持久化对象。
 */
public class DownloadTaskDO {

    /** 主键 ID */
    private Long id;

    /** 业务任务编码 */
    private String taskCode;

    /** 来源类型 */
    private String sourceType;

    /** 来源链接或磁力 */
    private String sourceUri;

    /** 来源哈希 */
    private String sourceHash;

    /** 种子文件路径 */
    private String torrentFilePath;

    /** BT 文件列表 JSON */
    private String torrentFileListJson;

    /** 展示名称 */
    private String displayName;

    /** 领域状态 */
    private String domainStatus;

    /** 引擎状态快照 */
    private String engineStatus;

    /** aria2 任务标识 */
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

    /** 任务入口类型 */
    private String entryType;

    /** 来源 Provider 编码 */
    private String sourceProvider;

    /** 来源站点 Host */
    private String sourceSiteHost;

    /** 入口上下文 JSON */
    private String entryContextJson;

    /** 引擎配置模板编码 */
    private String engineProfileCode;

    /** 打开文件夹目录 */
    private String openFolderPath;

    /** 主文件路径 */
    private String primaryFilePath;

    /** 完成时间 */
    private Long completedAt;

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

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public void setSourceProvider(String sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    public String getSourceSiteHost() {
        return sourceSiteHost;
    }

    public void setSourceSiteHost(String sourceSiteHost) {
        this.sourceSiteHost = sourceSiteHost;
    }

    public String getEntryContextJson() {
        return entryContextJson;
    }

    public void setEntryContextJson(String entryContextJson) {
        this.entryContextJson = entryContextJson;
    }

    public String getEngineProfileCode() {
        return engineProfileCode;
    }

    public void setEngineProfileCode(String engineProfileCode) {
        this.engineProfileCode = engineProfileCode;
    }

    public String getOpenFolderPath() {
        return openFolderPath;
    }

    public void setOpenFolderPath(String openFolderPath) {
        this.openFolderPath = openFolderPath;
    }

    public String getPrimaryFilePath() {
        return primaryFilePath;
    }

    public void setPrimaryFilePath(String primaryFilePath) {
        this.primaryFilePath = primaryFilePath;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
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
