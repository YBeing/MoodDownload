package com.mooddownload.local.controller.config.vo;

import javax.validation.constraints.NotBlank;

/**
 * 更新 Tracker 配置集请求。
 */
public class UpdateBtTrackerSetRequest {

    /** Tracker 集名称 */
    @NotBlank(message = "不能为空")
    private String trackerSetName;

    /** Tracker 列表文本 */
    @NotBlank(message = "不能为空")
    private String trackerListText;

    /** 来源地址 */
    private String sourceUrl;

    public String getTrackerSetName() {
        return trackerSetName;
    }

    public void setTrackerSetName(String trackerSetName) {
        this.trackerSetName = trackerSetName;
    }

    public String getTrackerListText() {
        return trackerListText;
    }

    public void setTrackerListText(String trackerListText) {
        this.trackerListText = trackerListText;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
