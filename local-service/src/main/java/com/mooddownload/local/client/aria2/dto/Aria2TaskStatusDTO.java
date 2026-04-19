package com.mooddownload.local.client.aria2.dto;

import java.util.List;

/**
 * aria2 任务状态 DTO。
 */
public class Aria2TaskStatusDTO {

    /** aria2 任务 gid */
    private String gid;

    /** aria2 状态 */
    private String status;

    /** 总大小字符串 */
    private String totalLength;

    /** 已完成大小字符串 */
    private String completedLength;

    /** 下载速度字符串 */
    private String downloadSpeed;

    /** 上传速度字符串 */
    private String uploadSpeed;

    /** 错误码 */
    private String errorCode;

    /** 错误消息 */
    private String errorMessage;

    /** 后续承接任务 gid 列表 */
    private List<String> followedBy;

    /** 前置依赖任务 gid */
    private String belongsTo;

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(String totalLength) {
        this.totalLength = totalLength;
    }

    public String getCompletedLength() {
        return completedLength;
    }

    public void setCompletedLength(String completedLength) {
        this.completedLength = completedLength;
    }

    public String getDownloadSpeed() {
        return downloadSpeed;
    }

    public void setDownloadSpeed(String downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    public String getUploadSpeed() {
        return uploadSpeed;
    }

    public void setUploadSpeed(String uploadSpeed) {
        this.uploadSpeed = uploadSpeed;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getFollowedBy() {
        return followedBy;
    }

    public void setFollowedBy(List<String> followedBy) {
        this.followedBy = followedBy;
    }

    public String getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(String belongsTo) {
        this.belongsTo = belongsTo;
    }
}
