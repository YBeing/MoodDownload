package com.mooddownload.local.service.task.state;

/**
 * 任务状态流转触发器枚举。
 */
public enum TaskTriggerType {

    /** 创建任务 */
    CREATE,

    /** 调度抢占 */
    DISPATCH,

    /** 调度成功 */
    DISPATCH_SUCCESS,

    /** 调度失败但保留待重试 */
    DISPATCH_FAILED_RETRY,

    /** 调度失败且转为失败态 */
    DISPATCH_FAILED_FINAL,

    /** 暂停任务 */
    PAUSE,

    /** 继续任务 */
    RESUME,

    /** 取消任务 */
    CANCEL,

    /** 重试任务 */
    RETRY,

    /** 引擎轮询同步 */
    ENGINE_SYNC
}
