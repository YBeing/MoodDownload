package com.mooddownload.local.service.config.model;

/**
 * 下载配置领域模型。
 */
public class DownloadConfigModel {

    /** 默认保存目录 */
    private String defaultSaveDir;

    /** 最大并发下载数 */
    private Integer maxConcurrentDownloads;

    /** 全局最大下载限速 */
    private Integer maxGlobalDownloadSpeed;

    /** 全局最大上传限速 */
    private Integer maxGlobalUploadSpeed;

    /** 浏览器接管开关 */
    private Boolean browserCaptureEnabled;

    /** 剪贴板监听开关 */
    private Boolean clipboardMonitorEnabled;

    /** 自动启动开关 */
    private Boolean autoStartEnabled;

    /** 当前生效的引擎配置模板编码 */
    private String activeEngineProfileCode;

    /** 删除文件是否优先进入回收站 */
    private Boolean deleteToRecycleBinEnabled;

    /** 更新时间 */
    private Long updatedAt;

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

    public Boolean getBrowserCaptureEnabled() {
        return browserCaptureEnabled;
    }

    public void setBrowserCaptureEnabled(Boolean browserCaptureEnabled) {
        this.browserCaptureEnabled = browserCaptureEnabled;
    }

    public Boolean getClipboardMonitorEnabled() {
        return clipboardMonitorEnabled;
    }

    public void setClipboardMonitorEnabled(Boolean clipboardMonitorEnabled) {
        this.clipboardMonitorEnabled = clipboardMonitorEnabled;
    }

    public Boolean getAutoStartEnabled() {
        return autoStartEnabled;
    }

    public void setAutoStartEnabled(Boolean autoStartEnabled) {
        this.autoStartEnabled = autoStartEnabled;
    }

    public String getActiveEngineProfileCode() {
        return activeEngineProfileCode;
    }

    public void setActiveEngineProfileCode(String activeEngineProfileCode) {
        this.activeEngineProfileCode = activeEngineProfileCode;
    }

    public Boolean getDeleteToRecycleBinEnabled() {
        return deleteToRecycleBinEnabled;
    }

    public void setDeleteToRecycleBinEnabled(Boolean deleteToRecycleBinEnabled) {
        this.deleteToRecycleBinEnabled = deleteToRecycleBinEnabled;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
