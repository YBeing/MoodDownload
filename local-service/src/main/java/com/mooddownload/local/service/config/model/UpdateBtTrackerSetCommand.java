package com.mooddownload.local.service.config.model;

/**
 * 更新 Tracker 配置集命令。
 */
public class UpdateBtTrackerSetCommand {

    /** Tracker 集编码 */
    private String trackerSetCode;

    /** Tracker 集名称 */
    private String trackerSetName;

    /** Tracker 列表文本 */
    private String trackerListText;

    /** 来源地址 */
    private String sourceUrl;

    public String getTrackerSetCode() {
        return trackerSetCode;
    }

    public void setTrackerSetCode(String trackerSetCode) {
        this.trackerSetCode = trackerSetCode;
    }

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
