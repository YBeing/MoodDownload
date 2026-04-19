package com.mooddownload.local.service.task;

import com.mooddownload.local.service.task.model.TaskOperationResult;

/**
 * 删除任务执行结果。
 */
public class TaskDeleteExecutionResult {

    /** 删除后的任务结果 */
    private final TaskOperationResult taskOperationResult;

    /** 本地文件是否一并删除 */
    private final boolean filesRemoved;

    public TaskDeleteExecutionResult(TaskOperationResult taskOperationResult, boolean filesRemoved) {
        this.taskOperationResult = taskOperationResult;
        this.filesRemoved = filesRemoved;
    }

    public TaskOperationResult getTaskOperationResult() {
        return taskOperationResult;
    }

    public boolean isFilesRemoved() {
        return filesRemoved;
    }
}
