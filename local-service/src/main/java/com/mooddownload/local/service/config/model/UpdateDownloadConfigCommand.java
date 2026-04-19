package com.mooddownload.local.service.config.model;

/**
 * 更新下载配置命令。
 */
public class UpdateDownloadConfigCommand {

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
}
