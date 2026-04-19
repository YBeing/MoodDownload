package com.mooddownload.local.service.capture.model;

/**
 * 剪贴板接入命令。
 */
public class ClipboardCaptureCommand {

    /** 幂等请求键 */
    private String clientRequestId;

    /** 剪贴板原始文本 */
    private String clipboardText;

    /** 建议展示名称 */
    private String suggestedName;

    /** 调用方类型 */
    private String clientType;

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    public String getClipboardText() {
        return clipboardText;
    }

    public void setClipboardText(String clipboardText) {
        this.clipboardText = clipboardText;
    }

    public String getSuggestedName() {
        return suggestedName;
    }

    public void setSuggestedName(String suggestedName) {
        this.suggestedName = suggestedName;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }
}
