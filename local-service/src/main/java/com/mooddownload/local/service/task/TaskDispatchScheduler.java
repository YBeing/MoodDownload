package com.mooddownload.local.service.task;

import com.mooddownload.local.client.aria2.Aria2Properties;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.dal.task.DownloadAttemptRepository;
import com.mooddownload.local.mapper.task.DownloadAttemptDO;
import com.mooddownload.local.service.engine.Aria2CommandService;
import com.mooddownload.local.service.engine.model.EngineDispatchResult;
import com.mooddownload.local.service.task.model.BtTaskAggregateSnapshot;
import com.mooddownload.local.service.task.model.DownloadEngineTaskModel;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.model.TaskOperationResult;
import com.mooddownload.local.service.task.model.TorrentFileItem;
import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import com.mooddownload.local.service.task.state.TaskSourceType;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 待调度任务扫描与引擎分发服务。
 *
 * <p>单个任务的一次分发通过 TransactionTemplate 包裹，保证任务状态日志与尝试记录尽量原子。
 */
@Component
public class TaskDispatchScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskDispatchScheduler.class);

    private final TaskQueryService taskQueryService;

    private final TaskCommandService taskCommandService;

    private final Aria2CommandService aria2CommandService;

    private final DownloadAttemptRepository downloadAttemptRepository;

    private final TaskEventPublisher taskEventPublisher;

    private final TorrentFileListService torrentFileListService;

    private final Aria2Properties aria2Properties;

    private final TransactionTemplate transactionTemplate;

    public TaskDispatchScheduler(
        TaskQueryService taskQueryService,
        TaskCommandService taskCommandService,
        Aria2CommandService aria2CommandService,
        DownloadAttemptRepository downloadAttemptRepository,
        TaskEventPublisher taskEventPublisher,
        TorrentFileListService torrentFileListService,
        Aria2Properties aria2Properties,
        org.springframework.transaction.PlatformTransactionManager platformTransactionManager
    ) {
        this.taskQueryService = taskQueryService;
        this.taskCommandService = taskCommandService;
        this.aria2CommandService = aria2CommandService;
        this.downloadAttemptRepository = downloadAttemptRepository;
        this.taskEventPublisher = taskEventPublisher;
        this.torrentFileListService = torrentFileListService;
        this.aria2Properties = aria2Properties;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
    }

    /**
     * 扫描待调度任务并提交到 aria2。
     */
    public void dispatchPendingTasks() {
        List<DownloadTaskModel> dispatchableTasks = taskQueryService.listDispatchableTasks(
            aria2Properties.getDispatchBatchSize()
        );
        LOGGER.info("扫描待调度任务完成: size={}", dispatchableTasks.size());
        for (DownloadTaskModel dispatchableTask : dispatchableTasks) {
            transactionTemplate.executeWithoutResult(status -> dispatchSingleTask(dispatchableTask));
        }
    }

    /**
     * 立即分发指定任务，供创建/恢复/重试成功后的同步提交通路复用。
     *
     * @param taskId 任务 ID
     */
    public void dispatchTask(Long taskId) {
        if (taskId == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "taskId 不能为空");
        }
        transactionTemplate.executeWithoutResult(status -> {
            DownloadTaskModel downloadTaskModel = taskQueryService.getTaskById(taskId);
            if (!DownloadTaskStatus.PENDING.name().equals(downloadTaskModel.getDomainStatus())) {
                LOGGER.info("跳过立即分发，当前任务不处于待调度状态: taskId={}, domainStatus={}",
                    taskId, downloadTaskModel.getDomainStatus());
                return;
            }
            dispatchSingleTask(downloadTaskModel);
        });
    }

    private void dispatchSingleTask(DownloadTaskModel downloadTaskModel) {
        long now = System.currentTimeMillis();
        int attemptNo = downloadAttemptRepository.listByTaskId(downloadTaskModel.getId()).size() + 1;
        TaskOperationResult dispatchingResult = taskCommandService.markDispatching(downloadTaskModel.getId());
        publishTaskUpdated(dispatchingResult);
        try {
            EngineDispatchResult engineDispatchResult = aria2CommandService.createDownload(downloadTaskModel);
            TaskOperationResult dispatchSuccessResult = taskCommandService.markDispatchSucceeded(
                downloadTaskModel.getId(),
                engineDispatchResult.getEngineGid()
            );
            enrichTorrentFilesIfNecessary(dispatchSuccessResult.getTaskModel(), engineDispatchResult.getEngineGid());
            publishTaskUpdated(dispatchSuccessResult);
            downloadAttemptRepository.save(buildAttemptRecord(
                downloadTaskModel.getId(),
                attemptNo,
                "DISPATCH",
                "SUCCESS",
                engineDispatchResult.getEngineGid(),
                null,
                null,
                now
            ));
        } catch (BizException exception) {
            TaskOperationResult dispatchFailedResult = isDuplicateBtRegistration(exception)
                ? taskCommandService.markDispatchFinalFailure(
                    downloadTaskModel.getId(),
                    ErrorCode.STATE_CONFLICT.getCode(),
                    "BT 任务已存在于下载引擎，请勿重复导入"
                )
                : taskCommandService.markDispatchRetryableFailure(
                    downloadTaskModel.getId(),
                    exception.getErrorCode().getCode(),
                    exception.getMessage()
                );
            publishTaskUpdated(dispatchFailedResult);
            downloadAttemptRepository.save(buildAttemptRecord(
                downloadTaskModel.getId(),
                attemptNo,
                "DISPATCH",
                "FAILED",
                null,
                "DISPATCH",
                exception.getMessage(),
                now
            ));
            if (isDuplicateBtRegistration(exception)) {
                LOGGER.warn("任务提交到 aria2 命中重复 BT 任务，已转为失败态: taskId={}, message={}",
                    downloadTaskModel.getId(), exception.getMessage());
            } else {
                LOGGER.warn("任务提交到 aria2 失败，已回退为待调度: taskId={}, code={}, message={}",
                    downloadTaskModel.getId(), exception.getErrorCode().getCode(), exception.getMessage());
            }
        } catch (RuntimeException exception) {
            TaskOperationResult dispatchFailedResult = taskCommandService.markDispatchRetryableFailure(
                downloadTaskModel.getId(),
                ErrorCode.EXTERNAL_ENGINE_ERROR.getCode(),
                "aria2 分发发生未预期异常"
            );
            publishTaskUpdated(dispatchFailedResult);
            downloadAttemptRepository.save(buildAttemptRecord(
                downloadTaskModel.getId(),
                attemptNo,
                "DISPATCH",
                "FAILED",
                null,
                "DISPATCH",
                exception.getMessage(),
                now
            ));
            LOGGER.error("任务提交到 aria2 发生未预期异常: taskId={}", downloadTaskModel.getId(), exception);
        }
    }

    private void enrichTorrentFilesIfNecessary(DownloadTaskModel downloadTaskModel, String engineGid) {
        if (downloadTaskModel == null) {
            return;
        }
        if (!supportsBtFileList(TaskSourceType.fromCode(downloadTaskModel.getSourceType()))) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            List<TorrentFileItem> torrentFiles = torrentFileListService.fetchTorrentFiles(engineGid);
            boolean metadataOnly = TaskSourceType.fromCode(downloadTaskModel.getSourceType()) == TaskSourceType.MAGNET
                && (downloadTaskModel.getTotalSizeBytes() == null || downloadTaskModel.getTotalSizeBytes() <= 0L);
            DownloadEngineTaskModel downloadEngineTaskModel = new DownloadEngineTaskModel();
            downloadEngineTaskModel.setTaskId(downloadTaskModel.getId());
            downloadEngineTaskModel.setEngineGid(engineGid);
            downloadEngineTaskModel.setParentEngineGid(null);
            downloadEngineTaskModel.setEngineStatus(downloadTaskModel.getEngineStatus());
            downloadEngineTaskModel.setMetadataOnly(metadataOnly);
            downloadEngineTaskModel.setTotalSizeBytes(downloadTaskModel.getTotalSizeBytes());
            downloadEngineTaskModel.setCompletedSizeBytes(downloadTaskModel.getCompletedSizeBytes());
            downloadEngineTaskModel.setDownloadSpeedBps(downloadTaskModel.getDownloadSpeedBps());
            downloadEngineTaskModel.setUploadSpeedBps(downloadTaskModel.getUploadSpeedBps());
            downloadEngineTaskModel.setErrorCode(downloadTaskModel.getErrorCode());
            downloadEngineTaskModel.setErrorMessage(downloadTaskModel.getErrorMessage());
            downloadEngineTaskModel.setTorrentFiles(torrentFiles);
            downloadEngineTaskModel.setCreatedAt(now);
            downloadEngineTaskModel.setUpdatedAt(now);

            BtTaskAggregateSnapshot btTaskAggregateSnapshot = new BtTaskAggregateSnapshot();
            btTaskAggregateSnapshot.setPrimaryEngineGid(engineGid);
            btTaskAggregateSnapshot.setDomainStatus(DownloadTaskStatus.valueOf(downloadTaskModel.getDomainStatus()));
            btTaskAggregateSnapshot.setEngineStatus(downloadTaskModel.getEngineStatus());
            btTaskAggregateSnapshot.setTotalSizeBytes(downloadTaskModel.getTotalSizeBytes());
            btTaskAggregateSnapshot.setCompletedSizeBytes(downloadTaskModel.getCompletedSizeBytes());
            btTaskAggregateSnapshot.setDownloadSpeedBps(downloadTaskModel.getDownloadSpeedBps());
            btTaskAggregateSnapshot.setUploadSpeedBps(downloadTaskModel.getUploadSpeedBps());
            btTaskAggregateSnapshot.setErrorCode(downloadTaskModel.getErrorCode());
            btTaskAggregateSnapshot.setErrorMessage(downloadTaskModel.getErrorMessage());
            btTaskAggregateSnapshot.setEngineTasks(Collections.singletonList(downloadEngineTaskModel));
            btTaskAggregateSnapshot.setTorrentFiles(torrentFiles);

            publishTaskUpdated(taskCommandService.syncBtTaskAggregate(
                downloadTaskModel.getId(),
                btTaskAggregateSnapshot
            ));
        } catch (RuntimeException exception) {
            LOGGER.warn("补充 BT 文件列表失败，不影响任务主流程: taskId={}, gid={}",
                downloadTaskModel.getId(), engineGid, exception);
        }
    }

    private boolean supportsBtFileList(TaskSourceType taskSourceType) {
        return taskSourceType == TaskSourceType.TORRENT
            || taskSourceType == TaskSourceType.MAGNET
            || taskSourceType == TaskSourceType.BT;
    }

    private boolean isDuplicateBtRegistration(BizException exception) {
        return exception != null
            && exception.getMessage() != null
            && exception.getMessage().toLowerCase().contains("already registered");
    }

    private void publishTaskUpdated(TaskOperationResult taskOperationResult) {
        if (taskOperationResult != null && taskOperationResult.getTaskModel() != null) {
            taskEventPublisher.publishTaskUpdated(taskOperationResult.getTaskModel());
        }
    }

    private DownloadAttemptDO buildAttemptRecord(
        Long taskId,
        Integer attemptNo,
        String triggerReason,
        String resultStatus,
        String engineGid,
        String failPhase,
        String failMessage,
        long now
    ) {
        DownloadAttemptDO downloadAttemptDO = new DownloadAttemptDO();
        downloadAttemptDO.setTaskId(taskId);
        downloadAttemptDO.setAttemptNo(attemptNo);
        downloadAttemptDO.setTriggerReason(triggerReason);
        downloadAttemptDO.setResultStatus(resultStatus);
        downloadAttemptDO.setEngineGid(engineGid);
        downloadAttemptDO.setFailPhase(failPhase);
        downloadAttemptDO.setFailMessage(failMessage);
        downloadAttemptDO.setStartedAt(now);
        downloadAttemptDO.setFinishedAt(now);
        downloadAttemptDO.setCreatedAt(now);
        downloadAttemptDO.setUpdatedAt(now);
        return downloadAttemptDO;
    }
}
