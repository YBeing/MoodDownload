package com.mooddownload.local.service.task.model;

import java.nio.file.Path;
import java.util.List;

/**
 * 任务文件清理计划。
 */
public class TaskFileCleanupPlan {

    /** 输出文件目标 */
    private List<Path> outputTargets;

    /** 关联工件目标 */
    private List<Path> artifactTargets;

    /** 全部目标 */
    private List<Path> allTargets;

    public List<Path> getOutputTargets() {
        return outputTargets;
    }

    public void setOutputTargets(List<Path> outputTargets) {
        this.outputTargets = outputTargets;
    }

    public List<Path> getArtifactTargets() {
        return artifactTargets;
    }

    public void setArtifactTargets(List<Path> artifactTargets) {
        this.artifactTargets = artifactTargets;
    }

    public List<Path> getAllTargets() {
        return allTargets;
    }

    public void setAllTargets(List<Path> allTargets) {
        this.allTargets = allTargets;
    }
}
