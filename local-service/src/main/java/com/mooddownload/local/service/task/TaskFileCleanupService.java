package com.mooddownload.local.service.task;

import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.model.TaskDeleteMode;
import com.mooddownload.local.service.task.model.TaskFileCleanupPlan;
import com.mooddownload.local.service.task.model.TaskFileCleanupResult;
import com.mooddownload.local.service.task.model.TaskOpenContextModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 任务文件清理服务。
 */
@Service
public class TaskFileCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskFileCleanupService.class);

    /**
     * 尝试删除任务关联的本地文件。
     *
     * @param downloadTaskModel 任务快照
     * @return 文件清理是否成功完成
     */
    public TaskFileCleanupResult cleanupTaskFiles(DownloadTaskModel downloadTaskModel, TaskDeleteMode deleteMode) {
        TaskFileCleanupPlan cleanupPlan = previewCleanupPlan(downloadTaskModel, deleteMode);
        if (cleanupPlan.getAllTargets().isEmpty()) {
            LOGGER.info("任务无可清理文件，跳过文件删除: taskId={}", downloadTaskModel == null ? null : downloadTaskModel.getId());
            TaskFileCleanupResult emptyResult = new TaskFileCleanupResult();
            emptyResult.setOutputRemoved(true);
            emptyResult.setArtifactRemoved(true);
            emptyResult.setPartialSuccess(false);
            return emptyResult;
        }
        boolean outputRemoved = true;
        boolean artifactRemoved = true;
        for (Path cleanupTarget : cleanupPlan.getOutputTargets()) {
            try {
                boolean deleted = Files.deleteIfExists(cleanupTarget);
                LOGGER.info("处理任务文件清理完成: path={}, deleted={}", cleanupTarget, deleted);
            } catch (IOException exception) {
                outputRemoved = false;
                LOGGER.error("删除任务文件失败: path={}", cleanupTarget, exception);
            }
        }
        for (Path cleanupTarget : cleanupPlan.getArtifactTargets()) {
            try {
                boolean deleted = Files.deleteIfExists(cleanupTarget);
                LOGGER.info("处理任务关联工件清理完成: path={}, deleted={}", cleanupTarget, deleted);
            } catch (IOException exception) {
                artifactRemoved = false;
                LOGGER.error("删除任务关联工件失败: path={}", cleanupTarget, exception);
            }
        }
        TaskFileCleanupResult cleanupResult = new TaskFileCleanupResult();
        cleanupResult.setOutputRemoved(outputRemoved);
        cleanupResult.setArtifactRemoved(artifactRemoved);
        cleanupResult.setPartialSuccess(!outputRemoved || !artifactRemoved);
        return cleanupResult;
    }

    /**
     * 预览文件清理计划。
     *
     * @param downloadTaskModel 任务快照
     * @param deleteMode 删除模式
     * @return 清理计划
     */
    public TaskFileCleanupPlan previewCleanupPlan(DownloadTaskModel downloadTaskModel, TaskDeleteMode deleteMode) {
        TaskFileCleanupPlan cleanupPlan = new TaskFileCleanupPlan();
        Set<Path> outputTargets = resolveOutputTargets(downloadTaskModel);
        Set<Path> artifactTargets = resolveArtifactTargets(downloadTaskModel);
        if (deleteMode == TaskDeleteMode.TASK_ONLY) {
            outputTargets.clear();
            artifactTargets.clear();
        } else if (deleteMode == TaskDeleteMode.TASK_AND_OUTPUT) {
            artifactTargets.clear();
        }
        Set<Path> allTargets = new LinkedHashSet<Path>();
        allTargets.addAll(outputTargets);
        allTargets.addAll(artifactTargets);
        cleanupPlan.setOutputTargets(new ArrayList<Path>(outputTargets));
        cleanupPlan.setArtifactTargets(new ArrayList<Path>(artifactTargets));
        cleanupPlan.setAllTargets(new ArrayList<Path>(allTargets));
        return cleanupPlan;
    }

    /**
     * 解析打开文件夹上下文。
     *
     * @param downloadTaskModel 任务快照
     * @return 打开上下文
     */
    public TaskOpenContextModel resolveOpenContext(DownloadTaskModel downloadTaskModel) {
        TaskOpenContextModel contextModel = new TaskOpenContextModel();
        contextModel.setTaskId(downloadTaskModel == null ? null : downloadTaskModel.getId());
        if (downloadTaskModel == null) {
            contextModel.setCanOpen(false);
            contextModel.setReason("任务不存在");
            return contextModel;
        }
        String primaryFilePath = resolvePrimaryFilePath(downloadTaskModel);
        String openFolderPath = resolveFolderPath(downloadTaskModel, primaryFilePath);
        contextModel.setPrimaryFilePath(primaryFilePath);
        contextModel.setOpenFolderPath(openFolderPath);
        contextModel.setCanOpen(StringUtils.hasText(openFolderPath));
        contextModel.setReason(StringUtils.hasText(openFolderPath) ? null : "未解析到可打开目录");
        return contextModel;
    }

    private Set<Path> resolveOutputTargets(DownloadTaskModel downloadTaskModel) {
        Set<Path> cleanupTargets = new LinkedHashSet<Path>();
        if (downloadTaskModel == null) {
            return cleanupTargets;
        }
        String primaryFilePath = resolvePrimaryFilePath(downloadTaskModel);
        if (StringUtils.hasText(primaryFilePath)) {
            cleanupTargets.add(Paths.get(primaryFilePath.trim()));
        }
        return cleanupTargets;
    }

    private Set<Path> resolveArtifactTargets(DownloadTaskModel downloadTaskModel) {
        Set<Path> cleanupTargets = new LinkedHashSet<Path>();
        if (downloadTaskModel == null) {
            return cleanupTargets;
        }
        if (StringUtils.hasText(downloadTaskModel.getTorrentFilePath())) {
            cleanupTargets.add(Paths.get(downloadTaskModel.getTorrentFilePath().trim()));
        }
        return cleanupTargets;
    }

    private String resolvePrimaryFilePath(DownloadTaskModel downloadTaskModel) {
        if (StringUtils.hasText(downloadTaskModel.getPrimaryFilePath())) {
            return downloadTaskModel.getPrimaryFilePath().trim();
        }
        if (downloadTaskModel.getTorrentFiles() != null) {
            for (com.mooddownload.local.service.task.model.TorrentFileItem torrentFileItem : downloadTaskModel.getTorrentFiles()) {
                if (torrentFileItem != null && torrentFileItem.getSelected() != null
                    && torrentFileItem.getSelected() && StringUtils.hasText(torrentFileItem.getFilePath())) {
                    return torrentFileItem.getFilePath().trim();
                }
            }
        }
        if (!StringUtils.hasText(downloadTaskModel.getSaveDir())) {
            return null;
        }
        String outputFileName = resolveOutputFileName(downloadTaskModel);
        if (!StringUtils.hasText(outputFileName)) {
            return null;
        }
        return Paths.get(downloadTaskModel.getSaveDir().trim(), outputFileName).toString();
    }

    private String resolveFolderPath(DownloadTaskModel downloadTaskModel, String primaryFilePath) {
        if (StringUtils.hasText(downloadTaskModel.getOpenFolderPath())) {
            return downloadTaskModel.getOpenFolderPath().trim();
        }
        if (StringUtils.hasText(primaryFilePath)) {
            Path primaryFile = Paths.get(primaryFilePath);
            Path parentPath = primaryFile.getParent();
            return parentPath == null ? null : parentPath.toString();
        }
        return StringUtils.hasText(downloadTaskModel.getSaveDir()) ? downloadTaskModel.getSaveDir().trim() : null;
    }

    private String resolveOutputFileName(DownloadTaskModel downloadTaskModel) {
        if (StringUtils.hasText(downloadTaskModel.getDisplayName())) {
            return downloadTaskModel.getDisplayName().trim();
        }
        if (StringUtils.hasText(downloadTaskModel.getTaskCode())) {
            return downloadTaskModel.getTaskCode().trim();
        }
        return null;
    }
}
