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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Locale;
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
                boolean deleted = deletePathIfExists(cleanupTarget);
                LOGGER.info("处理任务文件清理完成: path={}, deleted={}", cleanupTarget, deleted);
            } catch (IOException exception) {
                outputRemoved = false;
                LOGGER.error("删除任务文件失败: path={}", cleanupTarget, exception);
            }
        }
        for (Path cleanupTarget : cleanupPlan.getArtifactTargets()) {
            try {
                boolean deleted = deletePathIfExists(cleanupTarget);
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
        collectTorrentFileTargets(downloadTaskModel, cleanupTargets);
        String primaryFilePath = resolvePrimaryFilePath(downloadTaskModel);
        if (StringUtils.hasText(primaryFilePath)) {
            cleanupTargets.add(Paths.get(primaryFilePath.trim()));
        }
        collectExistingFallbackTargets(downloadTaskModel, cleanupTargets);
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
        if (StringUtils.hasText(downloadTaskModel.getSourceUri())) {
            String sourceFileName = extractFileNameFromUri(downloadTaskModel.getSourceUri());
            if (StringUtils.hasText(sourceFileName)) {
                return sourceFileName;
            }
        }
        if (StringUtils.hasText(downloadTaskModel.getTaskCode())) {
            return downloadTaskModel.getTaskCode().trim();
        }
        return null;
    }

    private void collectTorrentFileTargets(DownloadTaskModel downloadTaskModel, Set<Path> cleanupTargets) {
        if (downloadTaskModel.getTorrentFiles() == null || downloadTaskModel.getTorrentFiles().isEmpty()) {
            return;
        }
        for (com.mooddownload.local.service.task.model.TorrentFileItem torrentFileItem : downloadTaskModel.getTorrentFiles()) {
            if (torrentFileItem == null || !Boolean.TRUE.equals(torrentFileItem.getSelected())
                || !StringUtils.hasText(torrentFileItem.getFilePath())) {
                continue;
            }
            cleanupTargets.add(Paths.get(torrentFileItem.getFilePath().trim()));
        }
    }

    private void collectExistingFallbackTargets(DownloadTaskModel downloadTaskModel, Set<Path> cleanupTargets) {
        if (!StringUtils.hasText(downloadTaskModel.getSaveDir())) {
            return;
        }
        Path saveDirectory = Paths.get(downloadTaskModel.getSaveDir().trim());
        if (!Files.isDirectory(saveDirectory)) {
            return;
        }
        String outputFileName = resolveOutputFileName(downloadTaskModel);
        if (StringUtils.hasText(outputFileName)) {
            addIfExists(cleanupTargets, saveDirectory.resolve(outputFileName));
        }
        String sourceFileName = extractFileNameFromUri(downloadTaskModel.getSourceUri());
        if (StringUtils.hasText(sourceFileName)) {
            addIfExists(cleanupTargets, saveDirectory.resolve(sourceFileName));
        }
        String displayName = normalizeFileName(downloadTaskModel.getDisplayName());
        if (StringUtils.hasText(displayName)) {
            addMatchingFilesByNamePrefix(cleanupTargets, saveDirectory, displayName);
        }
        if (StringUtils.hasText(sourceFileName)) {
            addMatchingFilesByNamePrefix(cleanupTargets, saveDirectory, sourceFileName);
        }
    }

    private void addIfExists(Set<Path> cleanupTargets, Path path) {
        if (path == null) {
            return;
        }
        Path normalizedPath = path.normalize();
        if (Files.exists(normalizedPath)) {
            cleanupTargets.add(normalizedPath);
        }
    }

    private void addMatchingFilesByNamePrefix(Set<Path> cleanupTargets, Path saveDirectory, String fileName) {
        String normalizedFileName = normalizeFileName(fileName);
        if (!StringUtils.hasText(normalizedFileName)) {
            return;
        }
        String lowerFileName = normalizedFileName.toLowerCase(Locale.ROOT);
        try (java.util.stream.Stream<Path> pathStream = Files.list(saveDirectory)) {
            pathStream
                .filter(Files::isRegularFile)
                .filter(path -> matchesFileName(path.getFileName().toString(), lowerFileName))
                .forEach(cleanupTargets::add);
        } catch (IOException exception) {
            LOGGER.warn("扫描下载目录匹配源文件失败: saveDir={}, fileName={}", saveDirectory, normalizedFileName, exception);
        }
    }

    /**
     * 判断下载目录内的候选文件是否与任务文件名匹配。
     *
     * @param candidateFileName 候选文件名
     * @param lowerExpectedFileName 已转小写的期望文件名
     * @return 是否可以作为任务源文件清理
     */
    private boolean matchesFileName(String candidateFileName, String lowerExpectedFileName) {
        if (!StringUtils.hasText(candidateFileName) || !StringUtils.hasText(lowerExpectedFileName)) {
            return false;
        }
        String lowerCandidateFileName = candidateFileName.trim().toLowerCase(Locale.ROOT);
        return lowerCandidateFileName.equals(lowerExpectedFileName)
            || lowerCandidateFileName.startsWith(lowerExpectedFileName + ".")
            || lowerCandidateFileName.startsWith(lowerExpectedFileName + "_")
            || lowerCandidateFileName.startsWith(lowerExpectedFileName + "-");
    }

    /**
     * 从来源地址解析真实下载文件名。
     *
     * @param sourceUri 任务来源地址
     * @return URL 路径中的文件名，无法解析时返回 null
     */
    private String extractFileNameFromUri(String sourceUri) {
        if (!StringUtils.hasText(sourceUri)) {
            return null;
        }
        try {
            java.net.URI uri = java.net.URI.create(sourceUri.trim());
            String path = uri.getPath();
            if (!StringUtils.hasText(path)) {
                return null;
            }
            return normalizeFileName(Paths.get(path).getFileName().toString());
        } catch (RuntimeException exception) {
            LOGGER.debug("解析来源 URI 文件名失败: sourceUri={}", sourceUri, exception);
            return null;
        }
    }

    private String normalizeFileName(String fileName) {
        return StringUtils.hasText(fileName) ? fileName.trim() : null;
    }

    /**
     * 删除文件或目录。
     *
     * @param cleanupTarget 清理目标路径
     * @return 是否实际删除了文件系统对象
     * @throws IOException 当文件系统删除失败时抛出
     */
    private boolean deletePathIfExists(Path cleanupTarget) throws IOException {
        if (cleanupTarget == null) {
            return false;
        }
        Path normalizedTarget = cleanupTarget.normalize();
        if (Files.isDirectory(normalizedTarget)) {
            try (java.util.stream.Stream<Path> pathStream = Files.walk(normalizedTarget)) {
                java.util.List<Path> paths = pathStream
                    .sorted(Comparator.reverseOrder())
                    .collect(java.util.stream.Collectors.toList());
                boolean deleted = false;
                for (Path path : paths) {
                    deleted |= Files.deleteIfExists(path);
                }
                return deleted;
            }
        }
        return Files.deleteIfExists(normalizedTarget);
    }
}
