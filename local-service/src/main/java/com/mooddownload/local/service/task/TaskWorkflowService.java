package com.mooddownload.local.service.task;

import com.mooddownload.local.service.engine.Aria2CommandService;
import com.mooddownload.local.service.task.model.CreateTaskCommand;
import com.mooddownload.local.service.task.model.DownloadEngineTaskModel;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.model.TaskDeleteMode;
import com.mooddownload.local.service.task.model.TaskDeletionPreview;
import com.mooddownload.local.service.task.model.TaskFileCleanupPlan;
import com.mooddownload.local.service.task.model.TaskFileCleanupResult;
import com.mooddownload.local.service.task.model.TaskOpenContextModel;
import com.mooddownload.local.service.task.model.TaskOperationResult;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 任务工作流编排服务。
 */
@Service
public class TaskWorkflowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskWorkflowService.class);

    private final TaskCommandService taskCommandService;

    private final TaskQueryService taskQueryService;

    private final TaskDispatchScheduler taskDispatchScheduler;

    private final TaskFileCleanupService taskFileCleanupService;

    private final Aria2CommandService aria2CommandService;

    private final TaskEventPublisher taskEventPublisher;

    private final com.mooddownload.local.dal.task.TaskDeletionLogRepository taskDeletionLogRepository;

    public TaskWorkflowService(
        TaskCommandService taskCommandService,
        TaskQueryService taskQueryService,
        TaskDispatchScheduler taskDispatchScheduler,
        TaskFileCleanupService taskFileCleanupService,
        Aria2CommandService aria2CommandService,
        TaskEventPublisher taskEventPublisher,
        com.mooddownload.local.dal.task.TaskDeletionLogRepository taskDeletionLogRepository
    ) {
        this.taskCommandService = taskCommandService;
        this.taskQueryService = taskQueryService;
        this.taskDispatchScheduler = taskDispatchScheduler;
        this.taskFileCleanupService = taskFileCleanupService;
        this.aria2CommandService = aria2CommandService;
        this.taskEventPublisher = taskEventPublisher;
        this.taskDeletionLogRepository = taskDeletionLogRepository;
    }

    /**
     * 创建任务并立即尝试分发到 aria2。
     *
     * @param command 创建命令
     * @return 最新任务结果
     */
    public TaskOperationResult createTask(CreateTaskCommand command) {
        TaskOperationResult taskOperationResult = taskCommandService.createTask(command);
        publishTaskEventIfNecessary(taskOperationResult);
        if (!taskOperationResult.isIdempotent()) {
            taskDispatchScheduler.dispatchTask(taskOperationResult.getTaskModel().getId());
        }
        return refreshTaskOperationResult(taskOperationResult);
    }

    /**
     * 恢复任务并立即尝试重新分发。
     *
     * @param taskId 任务 ID
     * @return 最新任务结果
     */
    public TaskOperationResult resumeTask(Long taskId) {
        DownloadTaskModel currentTask = taskQueryService.getTaskById(taskId);
        resumeEngineTasksIfNecessary(currentTask);
        TaskOperationResult taskOperationResult = taskCommandService.resumeTask(taskId);
        publishTaskEventIfNecessary(taskOperationResult);
        if (!taskOperationResult.isIdempotent()
            && "PENDING".equals(taskOperationResult.getTaskModel().getDomainStatus())) {
            taskDispatchScheduler.dispatchTask(taskOperationResult.getTaskModel().getId());
        }
        return refreshTaskOperationResult(taskOperationResult);
    }

    /**
     * 重试任务并立即尝试重新分发。
     *
     * @param taskId 任务 ID
     * @return 最新任务结果
     */
    public TaskOperationResult retryTask(Long taskId) {
        TaskOperationResult taskOperationResult = taskCommandService.retryTask(taskId);
        publishTaskEventIfNecessary(taskOperationResult);
        if (!taskOperationResult.isIdempotent()) {
            taskDispatchScheduler.dispatchTask(taskOperationResult.getTaskModel().getId());
        }
        return refreshTaskOperationResult(taskOperationResult);
    }

    /**
     * 暂停任务并发布变更事件。
     *
     * @param taskId 任务 ID
     * @return 最新任务结果
     */
    public TaskOperationResult pauseTask(Long taskId) {
        DownloadTaskModel currentTask = taskQueryService.getTaskById(taskId);
        pauseEngineTasksIfNecessary(currentTask);
        TaskOperationResult taskOperationResult = taskCommandService.pauseTask(taskId);
        publishTaskEventIfNecessary(taskOperationResult);
        return refreshTaskOperationResult(taskOperationResult);
    }

    /**
     * 删除任务，必要时停止 aria2 并清理本地文件。
     *
     * @param taskId 任务 ID
     * @param removeFiles 是否删除本地文件
     * @return 删除执行结果
     */
    public TaskDeleteExecutionResult deleteTask(Long taskId, TaskDeleteMode deleteMode) {
        DownloadTaskModel currentTask = taskQueryService.getTaskById(taskId);
        removeFromEngineIfNecessary(currentTask);
        TaskOperationResult taskOperationResult = taskCommandService.cancelTask(taskId);
        publishTaskEventIfNecessary(taskOperationResult);
        TaskFileCleanupResult cleanupResult = taskFileCleanupService.cleanupTaskFiles(currentTask, deleteMode);
        persistDeletionLog(currentTask, deleteMode, cleanupResult);
        return new TaskDeleteExecutionResult(
            refreshTaskOperationResult(taskOperationResult),
            deleteMode,
            cleanupResult.isOutputRemoved(),
            cleanupResult.isArtifactRemoved(),
            cleanupResult.isPartialSuccess()
        );
    }

    /**
     * 预览任务删除影响。
     *
     * @param taskId 任务 ID
     * @param deleteMode 删除模式
     * @return 删除预览
     */
    public TaskDeletionPreview previewDeletion(Long taskId, TaskDeleteMode deleteMode) {
        DownloadTaskModel currentTask = taskQueryService.getTaskById(taskId);
        TaskFileCleanupPlan cleanupPlan = taskFileCleanupService.previewCleanupPlan(currentTask, deleteMode);
        TaskDeletionPreview preview = new TaskDeletionPreview();
        preview.setTaskId(taskId);
        preview.setDeleteMode(deleteMode);
        preview.setTargets(cleanupPlan.getAllTargets().stream().map(java.nio.file.Path::toString).collect(Collectors.toList()));
        preview.setWarnings(buildDeletionWarnings(currentTask, deleteMode, cleanupPlan));
        preview.setRemovable(Boolean.TRUE);
        return preview;
    }

    /**
     * 获取打开文件夹上下文。
     *
     * @param taskId 任务 ID
     * @return 打开上下文
     */
    public TaskOpenContextModel getOpenContext(Long taskId) {
        DownloadTaskModel currentTask = taskQueryService.getTaskById(taskId);
        return taskFileCleanupService.resolveOpenContext(currentTask);
    }

    private TaskOperationResult refreshTaskOperationResult(TaskOperationResult taskOperationResult) {
        if (taskOperationResult == null || taskOperationResult.getTaskModel() == null) {
            return taskOperationResult;
        }
        DownloadTaskModel latestTask = taskQueryService.getTaskById(taskOperationResult.getTaskModel().getId());
        return new TaskOperationResult(
            latestTask,
            taskOperationResult.getTaskDomainEvent(),
            taskOperationResult.isIdempotent()
        );
    }

    private void publishTaskEventIfNecessary(TaskOperationResult taskOperationResult) {
        if (taskOperationResult != null
            && !taskOperationResult.isIdempotent()
            && taskOperationResult.getTaskModel() != null) {
            taskEventPublisher.publishTaskUpdated(taskOperationResult.getTaskModel());
        }
    }

    private void removeFromEngineIfNecessary(DownloadTaskModel downloadTaskModel) {
        if (downloadTaskModel == null) {
            return;
        }
        if ("CANCELLED".equals(downloadTaskModel.getDomainStatus())) {
            return;
        }
        Set<String> engineGids = new LinkedHashSet<>();
        if (StringUtils.hasText(downloadTaskModel.getEngineGid())) {
            engineGids.add(downloadTaskModel.getEngineGid().trim());
        }
        if (downloadTaskModel.getEngineTasks() != null) {
            for (DownloadEngineTaskModel downloadEngineTaskModel : downloadTaskModel.getEngineTasks()) {
                if (downloadEngineTaskModel != null && StringUtils.hasText(downloadEngineTaskModel.getEngineGid())) {
                    engineGids.add(downloadEngineTaskModel.getEngineGid().trim());
                }
            }
        }
        for (String engineGid : engineGids) {
            aria2CommandService.removeDownload(engineGid);
            LOGGER.info("删除任务前已移除 aria2 任务: taskId={}, gid={}",
                downloadTaskModel.getId(), engineGid);
        }
    }

    /**
     * 暂停已绑定到 aria2 的任务，避免仅修改本地状态后被下一轮引擎同步改回运行中。
     *
     * @param downloadTaskModel 当前任务快照
     */
    private void pauseEngineTasksIfNecessary(DownloadTaskModel downloadTaskModel) {
        if (downloadTaskModel == null || !"RUNNING".equals(downloadTaskModel.getDomainStatus())) {
            return;
        }
        for (String engineGid : collectEngineGids(downloadTaskModel)) {
            aria2CommandService.pauseDownload(engineGid);
            LOGGER.info("暂停任务前已暂停 aria2 任务: taskId={}, gid={}",
                downloadTaskModel.getId(), engineGid);
        }
    }

    /**
     * 恢复已绑定到 aria2 的任务，避免重新分发造成重复下载。
     *
     * @param downloadTaskModel 当前任务快照
     */
    private void resumeEngineTasksIfNecessary(DownloadTaskModel downloadTaskModel) {
        if (downloadTaskModel == null || !"PAUSED".equals(downloadTaskModel.getDomainStatus())) {
            return;
        }
        for (String engineGid : collectEngineGids(downloadTaskModel)) {
            aria2CommandService.resumeDownload(engineGid);
            LOGGER.info("恢复任务前已恢复 aria2 任务: taskId={}, gid={}",
                downloadTaskModel.getId(), engineGid);
        }
    }

    private Set<String> collectEngineGids(DownloadTaskModel downloadTaskModel) {
        Set<String> engineGids = new LinkedHashSet<>();
        if (downloadTaskModel == null) {
            return engineGids;
        }
        if (StringUtils.hasText(downloadTaskModel.getEngineGid())) {
            engineGids.add(downloadTaskModel.getEngineGid().trim());
        }
        if (downloadTaskModel.getEngineTasks() != null) {
            for (DownloadEngineTaskModel downloadEngineTaskModel : downloadTaskModel.getEngineTasks()) {
                if (downloadEngineTaskModel != null && StringUtils.hasText(downloadEngineTaskModel.getEngineGid())) {
                    engineGids.add(downloadEngineTaskModel.getEngineGid().trim());
                }
            }
        }
        return engineGids;
    }

    private List<String> buildDeletionWarnings(
        DownloadTaskModel currentTask,
        TaskDeleteMode deleteMode,
        TaskFileCleanupPlan cleanupPlan
    ) {
        java.util.List<String> warnings = new java.util.ArrayList<String>();
        if (deleteMode == TaskDeleteMode.TASK_ONLY) {
            warnings.add("仅删除任务记录，磁盘文件会保留");
        }
        if (!cleanupPlan.getOutputTargets().isEmpty()) {
            warnings.add("将尝试删除已下载输出文件");
        }
        if (!cleanupPlan.getArtifactTargets().isEmpty()) {
            warnings.add("将尝试删除种子等关联工件");
        }
        if ("RUNNING".equals(currentTask.getDomainStatus())) {
            warnings.add("任务正在运行，删除前会先从 aria2 移除");
        }
        return warnings;
    }

    private void persistDeletionLog(
        DownloadTaskModel currentTask,
        TaskDeleteMode deleteMode,
        TaskFileCleanupResult cleanupResult
    ) {
        com.mooddownload.local.mapper.task.TaskDeletionLogDO deletionLogDO =
            new com.mooddownload.local.mapper.task.TaskDeletionLogDO();
        deletionLogDO.setTaskId(currentTask.getId());
        deletionLogDO.setDeleteMode(deleteMode.name());
        deletionLogDO.setOutputRemoved(cleanupResult.isOutputRemoved() ? 1 : 0);
        deletionLogDO.setArtifactRemoved(cleanupResult.isArtifactRemoved() ? 1 : 0);
        deletionLogDO.setRecycleBinUsed(0);
        deletionLogDO.setResultStatus(cleanupResult.isPartialSuccess() ? "PARTIAL_SUCCESS" : "SUCCESS");
        deletionLogDO.setOperatorSource("USER");
        deletionLogDO.setCreatedAt(System.currentTimeMillis());
        taskDeletionLogRepository.insert(deletionLogDO);
    }
}
