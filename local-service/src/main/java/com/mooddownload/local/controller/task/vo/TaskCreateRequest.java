package com.mooddownload.local.controller.task.vo;

import javax.validation.constraints.NotBlank;

/**
 * 创建下载任务请求。
 */
public class TaskCreateRequest {

    /** 幂等请求键 */
    @NotBlank(message = "不能为空")
    private String clientRequestId;

    /** 来源类型 */
    @NotBlank(message = "不能为空")
    private String sourceType;

    /** 来源地址 */
    @NotBlank(message = "不能为空")
    private String sourceUri;

    /** 保存目录 */
    private String saveDir;

    /** 展示名称 */
    private String displayName;

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

    public String getSaveDir() {
        return saveDir;
    }

    public void setSaveDir(String saveDir) {
        this.saveDir = saveDir;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
