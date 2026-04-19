package com.mooddownload.local.service.task;

import com.mooddownload.local.service.task.model.DownloadTaskModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public boolean cleanupTaskFiles(DownloadTaskModel downloadTaskModel) {
        Set<Path> cleanupTargets = resolveCleanupTargets(downloadTaskModel);
        if (cleanupTargets.isEmpty()) {
            LOGGER.info("任务无可清理文件，跳过文件删除: taskId={}", downloadTaskModel == null ? null : downloadTaskModel.getId());
            return false;
        }
        boolean cleanupSucceeded = true;
        for (Path cleanupTarget : cleanupTargets) {
            try {
                boolean deleted = Files.deleteIfExists(cleanupTarget);
                LOGGER.info("处理任务文件清理完成: path={}, deleted={}", cleanupTarget, deleted);
            } catch (IOException exception) {
                cleanupSucceeded = false;
                LOGGER.error("删除任务文件失败: path={}", cleanupTarget, exception);
            }
        }
        return cleanupSucceeded;
    }

    private Set<Path> resolveCleanupTargets(DownloadTaskModel downloadTaskModel) {
        Set<Path> cleanupTargets = new LinkedHashSet<Path>();
        if (downloadTaskModel == null) {
            return cleanupTargets;
        }
        if (StringUtils.hasText(downloadTaskModel.getSaveDir())) {
            String outputFileName = resolveOutputFileName(downloadTaskModel);
            if (StringUtils.hasText(outputFileName)) {
                cleanupTargets.add(Paths.get(downloadTaskModel.getSaveDir().trim(), outputFileName));
            }
        }
        if (StringUtils.hasText(downloadTaskModel.getTorrentFilePath())) {
            cleanupTargets.add(Paths.get(downloadTaskModel.getTorrentFilePath().trim()));
        }
        return cleanupTargets;
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
