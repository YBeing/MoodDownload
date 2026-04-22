package com.mooddownload.local.mapper.task;

/**
 * 任务删除审计持久化对象。
 */
public class TaskDeletionLogDO {

    /** 主键 ID */
    private Long id;

    /** 任务 ID */
    private Long taskId;

    /** 删除模式 */
    private String deleteMode;

    /** 输出文件是否已删除 */
    private Integer outputRemoved;

    /** 关联工件是否已删除 */
    private Integer artifactRemoved;

    /** 是否使用回收站 */
    private Integer recycleBinUsed;

    /** 执行结果状态 */
    private String resultStatus;

    /** 操作来源 */
    private String operatorSource;

    /** 创建时间 */
    private Long createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getDeleteMode() {
        return deleteMode;
    }

    public void setDeleteMode(String deleteMode) {
        this.deleteMode = deleteMode;
    }

    public Integer getOutputRemoved() {
        return outputRemoved;
    }

    public void setOutputRemoved(Integer outputRemoved) {
        this.outputRemoved = outputRemoved;
    }

    public Integer getArtifactRemoved() {
        return artifactRemoved;
    }

    public void setArtifactRemoved(Integer artifactRemoved) {
        this.artifactRemoved = artifactRemoved;
    }

    public Integer getRecycleBinUsed() {
        return recycleBinUsed;
    }

    public void setRecycleBinUsed(Integer recycleBinUsed) {
        this.recycleBinUsed = recycleBinUsed;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getOperatorSource() {
        return operatorSource;
    }

    public void setOperatorSource(String operatorSource) {
        this.operatorSource = operatorSource;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
