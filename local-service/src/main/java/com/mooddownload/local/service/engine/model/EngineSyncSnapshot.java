package com.mooddownload.local.service.engine.model;

import java.util.List;

/**
 * 引擎同步批次快照。
 */
public class EngineSyncSnapshot {

    /** 活跃任务列表 */
    private final List<EngineTaskSnapshot> activeTasks;

    /** 等待任务列表 */
    private final List<EngineTaskSnapshot> waitingTasks;

    /** 停止任务列表 */
    private final List<EngineTaskSnapshot> stoppedTasks;

    public EngineSyncSnapshot(
        List<EngineTaskSnapshot> activeTasks,
        List<EngineTaskSnapshot> waitingTasks,
        List<EngineTaskSnapshot> stoppedTasks
    ) {
        this.activeTasks = activeTasks;
        this.waitingTasks = waitingTasks;
        this.stoppedTasks = stoppedTasks;
    }

    public List<EngineTaskSnapshot> getActiveTasks() {
        return activeTasks;
    }

    public List<EngineTaskSnapshot> getWaitingTasks() {
        return waitingTasks;
    }

    public List<EngineTaskSnapshot> getStoppedTasks() {
        return stoppedTasks;
    }
}
