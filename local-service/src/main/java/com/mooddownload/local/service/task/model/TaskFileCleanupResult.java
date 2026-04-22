package com.mooddownload.local.service.task.model;

/**
 * 任务文件清理结果。
 */
public class TaskFileCleanupResult {

    /** 输出文件是否全部删除成功 */
    private boolean outputRemoved;

    /** 关联工件是否全部删除成功 */
    private boolean artifactRemoved;

    /** 是否部分成功 */
    private boolean partialSuccess;

    public boolean isOutputRemoved() {
        return outputRemoved;
    }

    public void setOutputRemoved(boolean outputRemoved) {
        this.outputRemoved = outputRemoved;
    }

    public boolean isArtifactRemoved() {
        return artifactRemoved;
    }

    public void setArtifactRemoved(boolean artifactRemoved) {
        this.artifactRemoved = artifactRemoved;
    }

    public boolean isPartialSuccess() {
        return partialSuccess;
    }

    public void setPartialSuccess(boolean partialSuccess) {
        this.partialSuccess = partialSuccess;
    }
}
