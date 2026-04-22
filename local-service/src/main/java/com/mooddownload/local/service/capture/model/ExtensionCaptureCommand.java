package com.mooddownload.local.service.capture.model;

/**
 * 扩展接管命令。
 */
public class ExtensionCaptureCommand {

    /** 幂等请求键 */
    private String clientRequestId;

    /** 浏览器类型 */
    private String browser;

    /** 来源标签页地址 */
    private String tabUrl;

    /** 下载地址 */
    private String downloadUrl;

    /** 建议展示名称 */
    private String suggestedName;

    /** Referer */
    private String referer;

    /** User-Agent */
    private String userAgent;

    /** 请求头快照 JSON */
    private String headerSnapshotJson;

    /** 调用方类型 */
    private String clientType;

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getTabUrl() {
        return tabUrl;
    }

    public void setTabUrl(String tabUrl) {
        this.tabUrl = tabUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getSuggestedName() {
        return suggestedName;
    }

    public void setSuggestedName(String suggestedName) {
        this.suggestedName = suggestedName;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getHeaderSnapshotJson() {
        return headerSnapshotJson;
    }

    public void setHeaderSnapshotJson(String headerSnapshotJson) {
        this.headerSnapshotJson = headerSnapshotJson;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }
}
