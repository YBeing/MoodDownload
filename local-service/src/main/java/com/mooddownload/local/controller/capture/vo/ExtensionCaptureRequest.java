package com.mooddownload.local.controller.capture.vo;

import javax.validation.constraints.NotBlank;

/**
 * 扩展接管请求。
 */
public class ExtensionCaptureRequest {

    /** 幂等请求键 */
    @NotBlank(message = "不能为空")
    private String clientRequestId;

    /** 浏览器类型 */
    @NotBlank(message = "不能为空")
    private String browser;

    /** 来源页面地址 */
    private String tabUrl;

    /** 实际下载地址 */
    @NotBlank(message = "不能为空")
    private String downloadUrl;

    /** 浏览器建议文件名 */
    private String suggestedName;

    /** Referer */
    private String referer;

    /** User-Agent */
    private String userAgent;

    /** 请求头快照 JSON */
    private String headerSnapshotJson;

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
}
