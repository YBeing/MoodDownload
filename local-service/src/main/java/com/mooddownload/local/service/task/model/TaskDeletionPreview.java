package com.mooddownload.local.service.task.model;

import java.util.List;

/**
 * 任务删除预览模型。
 */
public class TaskDeletionPreview {

    /** 任务 ID */
    private Long taskId;

    /** 删除模式 */
    private TaskDeleteMode deleteMode;

    /** 影响目标列表 */
    private List<String> targets;

    /** 风险提示列表 */
    private List<String> warnings;

    /** 是否可删除 */
    private Boolean removable;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public TaskDeleteMode getDeleteMode() {
        return deleteMode;
    }

    public void setDeleteMode(TaskDeleteMode deleteMode) {
        this.deleteMode = deleteMode;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public Boolean getRemovable() {
        return removable;
    }

    public void setRemovable(Boolean removable) {
        this.removable = removable;
    }
}
