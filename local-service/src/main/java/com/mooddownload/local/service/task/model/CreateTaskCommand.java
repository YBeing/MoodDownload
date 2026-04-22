package com.mooddownload.local.service.task.model;

/**
 * 创建任务命令。
 */
public class CreateTaskCommand {

    /** 幂等请求键 */
    private String clientRequestId;

    /** 来源类型 */
    private String sourceType;

    /** 来源地址 */
    private String sourceUri;

    /** 来源哈希 */
    private String sourceHash;

    /** 种子文件路径 */
    private String torrentFilePath;

    /** 展示名称 */
    private String displayName;

    /** 保存目录 */
    private String saveDir;

    /** 最大重试次数 */
    private Integer maxRetryCount;

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

    /** 调用方类型 */
    private String clientType;

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getSaveDir() {
        return saveDir;
    }

    public void setSaveDir(String saveDir) {
        this.saveDir = saveDir;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
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

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }
}
