package com.mooddownload.local.mapper.profile;

/**
 * Tracker 配置集持久化对象。
 */
public class BtTrackerSetDO {

    /** Tracker 集编码 */
    private String trackerSetCode;

    /** Tracker 集名称 */
    private String trackerSetName;

    /** 来源类型 */
    private String sourceType;

    /** Tracker 列表文本 */
    private String trackerListText;

    /** Tracker 来源地址 */
    private String trackerSourceUrl;

    /** 是否内置 */
    private Integer isBuiltin;

    /** 创建时间 */
    private Long createdAt;

    /** 更新时间 */
    private Long updatedAt;

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

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTrackerListText() {
        return trackerListText;
    }

    public void setTrackerListText(String trackerListText) {
        this.trackerListText = trackerListText;
    }

    public String getTrackerSourceUrl() {
        return trackerSourceUrl;
    }

    public void setTrackerSourceUrl(String trackerSourceUrl) {
        this.trackerSourceUrl = trackerSourceUrl;
    }

    public Integer getIsBuiltin() {
        return isBuiltin;
    }

    public void setIsBuiltin(Integer isBuiltin) {
        this.isBuiltin = isBuiltin;
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
