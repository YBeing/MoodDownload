package com.mooddownload.local.service.task;

import com.mooddownload.local.service.engine.Aria2CommandService;
import com.mooddownload.local.service.task.model.CreateTaskCommand;
import com.mooddownload.local.service.task.model.DownloadEngineTaskModel;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.model.TaskOperationResult;
import java.util.LinkedHashSet;
import java.util.Set;
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

    public TaskWorkflowService(
        TaskCommandService taskCommandService,
        TaskQueryService taskQueryService,
        TaskDispatchScheduler taskDispatchScheduler,
        TaskFileCleanupService taskFileCleanupService,
        Aria2CommandService aria2CommandService,
        TaskEventPublisher taskEventPublisher
    ) {
        this.taskCommandService = taskCommandService;
        this.taskQueryService = taskQueryService;
        this.taskDispatchScheduler = taskDispatchScheduler;
        this.taskFileCleanupService = taskFileCleanupService;
        this.aria2CommandService = aria2CommandService;
        this.taskEventPublisher = taskEventPublisher;
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
        TaskOperationResult taskOperationResult = taskCommandService.resumeTask(taskId);
        publishTaskEventIfNecessary(taskOperationResult);
        if (!taskOperationResult.isIdempotent()) {
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
    public TaskDeleteExecutionResult deleteTask(Long taskId, boolean removeFiles) {
        DownloadTaskModel currentTask = taskQueryService.getTaskById(taskId);
        removeFromEngineIfNecessary(currentTask);
        TaskOperationResult taskOperationResult = taskCommandService.cancelTask(taskId);
        publishTaskEventIfNecessary(taskOperationResult);
        boolean filesRemoved = removeFiles && taskFileCleanupService.cleanupTaskFiles(currentTask);
        return new TaskDeleteExecutionResult(refreshTaskOperationResult(taskOperationResult), filesRemoved);
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
}
