package com.mooddownload.local.service.task.model;

import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import java.util.List;

/**
 * BT 任务聚合同步快照。
 */
public class BtTaskAggregateSnapshot {

    /** 主任务当前应关注的引擎 gid */
    private String primaryEngineGid;

    /** 聚合后的领域状态 */
    private DownloadTaskStatus domainStatus;

    /** 聚合后的引擎状态 */
    private String engineStatus;

    /** 聚合后的总大小 */
    private Long totalSizeBytes;

    /** 聚合后的已完成大小 */
    private Long completedSizeBytes;

    /** 聚合后的下载速度 */
    private Long downloadSpeedBps;

    /** 聚合后的上传速度 */
    private Long uploadSpeedBps;

    /** 聚合后的错误码 */
    private String errorCode;

    /** 聚合后的错误信息 */
    private String errorMessage;

    /** 子任务快照列表 */
    private List<DownloadEngineTaskModel> engineTasks;

    /** 合并后的 BT 文件列表 */
    private List<TorrentFileItem> torrentFiles;

    public String getPrimaryEngineGid() {
        return primaryEngineGid;
    }

    public void setPrimaryEngineGid(String primaryEngineGid) {
        this.primaryEngineGid = primaryEngineGid;
    }

    public DownloadTaskStatus getDomainStatus() {
        return domainStatus;
    }

    public void setDomainStatus(DownloadTaskStatus domainStatus) {
        this.domainStatus = domainStatus;
    }

    public String getEngineStatus() {
        return engineStatus;
    }

    public void setEngineStatus(String engineStatus) {
        this.engineStatus = engineStatus;
    }

    public Long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(Long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    public Long getCompletedSizeBytes() {
        return completedSizeBytes;
    }

    public void setCompletedSizeBytes(Long completedSizeBytes) {
        this.completedSizeBytes = completedSizeBytes;
    }

    public Long getDownloadSpeedBps() {
        return downloadSpeedBps;
    }

    public void setDownloadSpeedBps(Long downloadSpeedBps) {
        this.downloadSpeedBps = downloadSpeedBps;
    }

    public Long getUploadSpeedBps() {
        return uploadSpeedBps;
    }

    public void setUploadSpeedBps(Long uploadSpeedBps) {
        this.uploadSpeedBps = uploadSpeedBps;
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

    public List<DownloadEngineTaskModel> getEngineTasks() {
        return engineTasks;
    }

    public void setEngineTasks(List<DownloadEngineTaskModel> engineTasks) {
        this.engineTasks = engineTasks;
    }

    public List<TorrentFileItem> getTorrentFiles() {
        return torrentFiles;
    }

    public void setTorrentFiles(List<TorrentFileItem> torrentFiles) {
        this.torrentFiles = torrentFiles;
    }
}
