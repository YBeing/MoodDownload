package com.mooddownload.local.controller.provider.vo;

/**
 * 百度网盘预检请求。
 */
public class BaiduPanPreflightRequest {

    /** 分享链接 */
    private String shareUrl;

    /** 鉴权上下文 */
    private String authContext;

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public String getAuthContext() {
        return authContext;
    }

    public void setAuthContext(String authContext) {
        this.authContext = authContext;
    }
}
