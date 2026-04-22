package com.mooddownload.local.service.task;

import com.mooddownload.local.service.task.model.TaskOperationResult;
import com.mooddownload.local.service.task.model.TaskDeleteMode;

/**
 * 删除任务执行结果。
 */
public class TaskDeleteExecutionResult {

    /** 删除后的任务结果 */
    private final TaskOperationResult taskOperationResult;

    /** 删除模式 */
    private final TaskDeleteMode deleteMode;

    /** 输出文件是否删除成功 */
    private final boolean outputRemoved;

    /** 关联工件是否删除成功 */
    private final boolean artifactRemoved;

    /** 是否部分成功 */
    private final boolean partialSuccess;

    public TaskDeleteExecutionResult(
        TaskOperationResult taskOperationResult,
        TaskDeleteMode deleteMode,
        boolean outputRemoved,
        boolean artifactRemoved,
        boolean partialSuccess
    ) {
        this.taskOperationResult = taskOperationResult;
        this.deleteMode = deleteMode;
        this.outputRemoved = outputRemoved;
        this.artifactRemoved = artifactRemoved;
        this.partialSuccess = partialSuccess;
    }

    public TaskOperationResult getTaskOperationResult() {
        return taskOperationResult;
    }

    public TaskDeleteMode getDeleteMode() {
        return deleteMode;
    }

    public boolean isOutputRemoved() {
        return outputRemoved;
    }

    public boolean isArtifactRemoved() {
        return artifactRemoved;
    }

    public boolean isPartialSuccess() {
        return partialSuccess;
    }
}
