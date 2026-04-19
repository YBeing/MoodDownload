package com.mooddownload.local.mapper.config;

/**
 * 下载配置持久化对象。
 */
public class DownloadConfigDO {

    /** 单例配置主键 */
    private Integer id;

    /** 默认保存目录 */
    private String defaultSaveDir;

    /** 最大并发下载数 */
    private Integer maxConcurrentDownloads;

    /** 全局最大下载限速 */
    private Integer maxGlobalDownloadSpeed;

    /** 全局最大上传限速 */
    private Integer maxGlobalUploadSpeed;

    /** 浏览器接管开关 */
    private Integer browserCaptureEnabled;

    /** 剪贴板监听开关 */
    private Integer clipboardMonitorEnabled;

    /** 自动开始开关 */
    private Integer autoStartEnabled;

    /** 本地 API 令牌 */
    private String localApiToken;

    /** 创建时间 */
    private Long createdAt;

    /** 更新时间 */
    private Long updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDefaultSaveDir() {
        return defaultSaveDir;
    }

    public void setDefaultSaveDir(String defaultSaveDir) {
        this.defaultSaveDir = defaultSaveDir;
    }

    public Integer getMaxConcurrentDownloads() {
        return maxConcurrentDownloads;
    }

    public void setMaxConcurrentDownloads(Integer maxConcurrentDownloads) {
        this.maxConcurrentDownloads = maxConcurrentDownloads;
    }

    public Integer getMaxGlobalDownloadSpeed() {
        return maxGlobalDownloadSpeed;
    }

    public void setMaxGlobalDownloadSpeed(Integer maxGlobalDownloadSpeed) {
        this.maxGlobalDownloadSpeed = maxGlobalDownloadSpeed;
    }

    public Integer getMaxGlobalUploadSpeed() {
        return maxGlobalUploadSpeed;
    }

    public void setMaxGlobalUploadSpeed(Integer maxGlobalUploadSpeed) {
        this.maxGlobalUploadSpeed = maxGlobalUploadSpeed;
    }

    public Integer getBrowserCaptureEnabled() {
        return browserCaptureEnabled;
    }

    public void setBrowserCaptureEnabled(Integer browserCaptureEnabled) {
        this.browserCaptureEnabled = browserCaptureEnabled;
    }

    public Integer getClipboardMonitorEnabled() {
        return clipboardMonitorEnabled;
    }

    public void setClipboardMonitorEnabled(Integer clipboardMonitorEnabled) {
        this.clipboardMonitorEnabled = clipboardMonitorEnabled;
    }

    public Integer getAutoStartEnabled() {
        return autoStartEnabled;
    }

    public void setAutoStartEnabled(Integer autoStartEnabled) {
        this.autoStartEnabled = autoStartEnabled;
    }

    public String getLocalApiToken() {
        return localApiToken;
    }

    public void setLocalApiToken(String localApiToken) {
        this.localApiToken = localApiToken;
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
