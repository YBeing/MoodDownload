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

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }
}
