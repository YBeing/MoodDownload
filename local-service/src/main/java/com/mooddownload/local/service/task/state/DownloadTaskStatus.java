package com.mooddownload.local.service.task.state;

/**
 * 下载任务领域状态枚举。
 */
public enum DownloadTaskStatus {

    /** 已创建，待调度 */
    PENDING,

    /** 正在提交到下载引擎 */
    DISPATCHING,

    /** 正在下载 */
    RUNNING,

    /** 已暂停 */
    PAUSED,

    /** 下载失败 */
    FAILED,

    /** 下载完成 */
    COMPLETED,

    /** 已取消 */
    CANCELLED,

    /** 对账中 */
    RECONCILING
}
