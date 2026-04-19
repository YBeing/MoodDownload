package com.mooddownload.local.service.task;

import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.dal.task.DownloadEngineTaskRepository;
import com.mooddownload.local.dal.task.DownloadTaskRepository;
import com.mooddownload.local.dal.task.TaskStateLogRepository;
import com.mooddownload.local.mapper.task.DownloadTaskDO;
import com.mooddownload.local.service.engine.model.EngineTaskSnapshot;
import com.mooddownload.local.service.task.convert.DownloadEngineTaskModelConverter;
import com.mooddownload.local.service.task.convert.TaskModelConverter;
import com.mooddownload.local.service.task.event.TaskDomainEvent;
import com.mooddownload.local.service.task.model.BtTaskAggregateSnapshot;
import com.mooddownload.local.service.task.model.CreateTaskCommand;
import com.mooddownload.local.service.task.model.DownloadEngineTaskModel;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.model.TaskOperationResult;
import com.mooddownload.local.service.task.model.TorrentFileItem;
import java.util.Collections;
import java.util.List;
import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import com.mooddownload.local.service.task.state.TaskTriggerType;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 任务命令服务，负责落库与状态流转编排。
 */
@Service
public class TaskCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskCommandService.class);

    private final DownloadTaskRepository downloadTaskRepository;

    private final DownloadEngineTaskRepository downloadEngineTaskRepository;

    private final TaskStateLogRepository taskStateLogRepository;

    private final TaskStateMachineService taskStateMachineService;

    private final TaskModelConverter taskModelConverter;

    private final DownloadEngineTaskModelConverter downloadEngineTaskModelConverter;

    public TaskCommandService(
        DownloadTaskRepository downloadTaskRepository,
        DownloadEngineTaskRepository downloadEngineTaskRepository,
        TaskStateLogRepository taskStateLogRepository,
        TaskStateMachineService taskStateMachineService,
        TaskModelConverter taskModelConverter,
        DownloadEngineTaskModelConverter downloadEngineTaskModelConverter
    ) {
        this.downloadTaskRepository = downloadTaskRepository;
        this.downloadEngineTaskRepository = downloadEngineTaskRepository;
        this.taskStateLogRepository = taskStateLogRepository;
        this.taskStateMachineService = taskStateMachineService;
        this.taskModelConverter = taskModelConverter;
        this.downloadEngineTaskModelConverter = downloadEngineTaskModelConverter;
    }

    /**
     * 创建下载任务。
     *
     * @param command 创建任务命令
     * @return 任务执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskOperationResult createTask(CreateTaskCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "创建任务命令不能为空");
        }
        Optional<DownloadTaskDO> existingTaskOptional = downloadTaskRepository.findByClientRequestId(
            command.getClientRequestId()
        );
        if (existingTaskOptional.isPresent()) {
            DownloadTaskModel existingTaskModel = taskModelConverter.toModel(existingTaskOptional.get());
            LOGGER.info("创建任务命中幂等键: taskId={}, clientRequestId={}",
                existingTaskModel.getId(), existingTaskModel.getClientRequestId());
            return new TaskOperationResult(existingTaskModel, null, true);
        }
        if (StringUtils.hasText(command.getSourceHash())) {
            Optional<DownloadTaskDO> reusableTaskOptional = downloadTaskRepository.findReusableBySourceHash(
                command.getSourceHash().trim()
            );
            if (reusableTaskOptional.isPresent()) {
                DownloadTaskModel reusableTaskModel = taskModelConverter.toModel(reusableTaskOptional.get());
                LOGGER.info("创建任务命中 BT 来源去重: taskId={}, sourceHash={}, clientRequestId={}",
                    reusableTaskModel.getId(), command.getSourceHash(), command.getClientRequestId());
                return new TaskOperationResult(reusableTaskModel, null, true);
            }
        }

        long now = System.currentTimeMillis();
        DownloadTaskModel downloadTaskModel = taskStateMachineService.initializeTask(command, now);
        try {
            Long taskId = downloadTaskRepository.save(taskModelConverter.toDownloadTaskDO(downloadTaskModel));
            downloadTaskModel.setId(taskId);
        } catch (RuntimeException exception) {
            Optional<DownloadTaskDO> duplicatedTask = downloadTaskRepository.findByClientRequestId(
                command.getClientRequestId()
            );
            if (duplicatedTask.isPresent()) {
                DownloadTaskModel existingTaskModel = taskModelConverter.toModel(duplicatedTask.get());
                LOGGER.warn("创建任务并发命中幂等键，返回既有任务: taskId={}, clientRequestId={}",
                    existingTaskModel.getId(), existingTaskModel.getClientRequestId());
                return new TaskOperationResult(existingTaskModel, null, true);
            }
            LOGGER.error("创建下载任务失败: clientRequestId={}", command.getClientRequestId(), exception);
            throw new BizException(ErrorCode.TASK_CREATE_FAILED, "创建任务失败");
        }

        TaskDomainEvent taskDomainEvent = new TaskDomainEvent(
            downloadTaskModel.getId(),
            downloadTaskModel.getTaskCode(),
            null,
            DownloadTaskStatus.PENDING,
            TaskTriggerType.CREATE,
            now
        );
        taskStateLogRepository.save(taskModelConverter.toTaskStateLogDO(
            downloadTaskModel,
            taskDomainEvent,
            resolveTriggerSource(command.getClientType(), "SYSTEM"),
            "创建下载任务"
        ));
        LOGGER.info("创建下载任务成功: taskId={}, taskCode={}, clientRequestId={}, sourceType={}",
            downloadTaskModel.getId(),
            downloadTaskModel.getTaskCode(),
            downloadTaskModel.getClientRequestId(),
            downloadTaskModel.getSourceType());
        return new TaskOperationResult(downloadTaskModel, taskDomainEvent, false);
    }

    /**
     * 暂停任务。
     *
     * @param taskId 任务 ID
     * @return 任务执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskOperationResult pauseTask(Long taskId) {
        DownloadTaskModel downloadTaskModel = loadTaskOrThrow(taskId);
        if (DownloadTaskStatus.PAUSED.name().equals(downloadTaskModel.getDomainStatus())) {
            return new TaskOperationResult(downloadTaskModel, null, true);
        }
        return applyTransition(downloadTaskModel, TaskTriggerType.PAUSE, "USER", "手动暂停任务");
    }

    /**
     * 继续任务。
     *
     * @param taskId 任务 ID
     * @return 任务执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskOperationResult resumeTask(Long taskId) {
        DownloadTaskModel downloadTaskModel = loadTaskOrThrow(taskId);
        if (DownloadTaskStatus.RUNNING.name().equals(downloadTaskModel.getDomainStatus())) {
            return new TaskOperationResult(downloadTaskModel, null, true);
        }
        return applyTransition(downloadTaskModel, TaskTriggerType.RESUME, "USER", "手动继续任务");
    }

    /**
     * 重试失败任务。
     *
     * @param taskId 任务 ID
     * @return 任务执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskOperationResult retryTask(Long taskId) {
        DownloadTaskModel downloadTaskModel = loadTaskOrThrow(taskId);
        return applyTransition(downloadTaskModel, TaskTriggerType.RETRY, "USER", "手动重试任务");
    }

    /**
     * 取消任务。
     *
     * @param taskId 任务 ID
     * @return 任务执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskOperationResult cancelTask(Long taskId) {
        DownloadTaskModel downloadTaskModel = loadTaskOrThrow(taskId);
        if (DownloadTaskStatus.CANCELLED.name().equals(downloadTaskModel.getDomainStatus())) {
            return new TaskOperationResult(downloadTaskModel, null, true);
        }
        return applyTransition(downloadTaskModel, TaskTriggerType.CANCEL, "USER", "手动删除任务");
    }

    /**
     * 将待调度任务推进到调度中。
     *
     * @param taskId 任务 ID
     * @return 任务执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskOperationResult markDispatching(Long taskId) {
        DownloadTaskModel downloadTaskModel = loadTaskOrThrow(taskId);
        if (DownloadTaskStatus.DISPATCHING.name().equals(downloadTaskModel.getDomainStatus())) {
            return new TaskOperationResult(downloadTaskModel, null, true);
        }
        return applyTransition(downloadTaskModel, TaskTriggerType.DISPATCH, "ENGINE", "调度器抢占任务");
    }

    /**
     * 标记 aria2 提交成功并绑定引擎 gid。
     *
     * @param taskId 任务 ID
     * @param engineGid 引擎 gid
     * @return 任务执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskOperationResult markDispatchSucceeded(Long taskId, String engineGid) {
        if (!StringUtils.hasText(engineGid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "engineGid 不能为空");
        }
        DownloadTaskModel downloadTaskModel = loadTaskOrThrow(taskId);
        if (DownloadTaskStatus.RUNNING.name().equals(downloadTaskModel.getDomainStatus())
            && engineGid.equals(downloadTaskModel.getEngineGid())) {
            return new TaskOperationResult(downloadTaskModel, null, true);
        }
        downloadTaskModel.setEngineGid(engineGid.trim());
        return applyTransition(downloadTaskModel, TaskTriggerType.DISPATCH_SUCCESS, "ENGINE", "aria2 创建下载成功");
    }

    /**
     * 标记 aria2 提交失败但任务保留待调度。
     *
     * @param taskId 任务 ID
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @return 任务执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskOperationResult markDispatchRetryableFailure(Long taskId, String errorCode, String errorMessage) {
        DownloadTaskModel downloadTaskModel = loadTaskOrThrow(taskId);
        downloadTaskModel.setErrorCode(errorCode);
        downloadTaskModel.setErrorMessage(errorMessage);
        return applyTransition(
            downloadTaskModel,
            TaskTriggerType.DISPATCH_FAILED_RETRY,
            "ENGINE",
            "aria2 创建下载失败，保留待调度"
        );
    }

    /**
     * 标记 aria2 提交失败且任务转为失败态。
     *
     * @param taskId 任务 ID
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @return 任务执行结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskOperationResult markDispatchFinalFailure(Long taskId, String errorCode, String errorMessage) {
        DownloadTaskModel downloadTaskModel = loadTaskOrThrow(taskId);
        downloadTaskModel.setErrorCode(errorCode);
        downloadTaskModel.setErrorMessage(errorMessage);
        return applyTransition(
            downloadTaskModel,
            TaskTriggerType.DISPATCH_FAILED_FINAL,
            "ENGINE",
            "aria2 创建下载失败，任务转为失败"
        );
    }

    /**
     * 将 aria2 轮询快照回写到本地任务，并在状态变化时记录状态日志。
     *
     * @param taskId 任务 ID
     * @param engineTaskSnapshot 引擎快照
     * @param targetStatus 目标领域状态
     * @return 同步结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskOperationResult syncTaskSnapshot(
        Long taskId,
        EngineTaskSnapshot engineTaskSnapshot,
        DownloadTaskStatus targetStatus
    ) {
        if (engineTaskSnapshot == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "engineTaskSnapshot 不能为空");
        }
        if (targetStatus == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "targetStatus 不能为空");
        }
        DownloadTaskModel downloadTaskModel = loadTaskOrThrow(taskId);
        if (DownloadTaskStatus.CANCELLED.name().equals(downloadTaskModel.getDomainStatus())) {
            LOGGER.info("任务已删除，忽略引擎同步结果: taskId={}, engineGid={}",
                downloadTaskModel.getId(), engineTaskSnapshot.getEngineGid());
            return new TaskOperationResult(downloadTaskModel, null, true);
        }

        long now = System.currentTimeMillis();
        DownloadTaskStatus fromStatus = DownloadTaskStatus.valueOf(downloadTaskModel.getDomainStatus());
        boolean changed = applySnapshot(downloadTaskModel, engineTaskSnapshot, targetStatus, now);
        if (!changed) {
            return new TaskOperationResult(downloadTaskModel, null, true);
        }

        TaskDomainEvent taskDomainEvent = null;
        if (fromStatus != targetStatus) {
            taskDomainEvent = new TaskDomainEvent(
                downloadTaskModel.getId(),
                downloadTaskModel.getTaskCode(),
                fromStatus,
                targetStatus,
                TaskTriggerType.ENGINE_SYNC,
                now
            );
        }

        downloadTaskRepository.updateCoreSnapshot(taskModelConverter.toDownloadTaskDO(downloadTaskModel));
        if (taskDomainEvent != null) {
            taskStateLogRepository.save(taskModelConverter.toTaskStateLogDO(
                downloadTaskModel,
                taskDomainEvent,
                "ENGINE",
                "aria2 轮询同步任务快照"
            ));
        }
        LOGGER.info("同步任务快照成功: taskId={}, engineGid={}, domainStatus={}, engineStatus={}",
            downloadTaskModel.getId(),
            downloadTaskModel.getEngineGid(),
            downloadTaskModel.getDomainStatus(),
            downloadTaskModel.getEngineStatus());
        return new TaskOperationResult(downloadTaskModel, taskDomainEvent, false);
    }

    /**
     * 回写 BT 聚合快照，同时替换任务下的全部子任务明细与合并文件列表。
     *
     * @param taskId 任务 ID
     * @param btTaskAggregateSnapshot BT 聚合同步快照
     * @return 同步结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskOperationResult syncBtTaskAggregate(Long taskId, BtTaskAggregateSnapshot btTaskAggregateSnapshot) {
        if (btTaskAggregateSnapshot == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "btTaskAggregateSnapshot 不能为空");
        }
        if (btTaskAggregateSnapshot.getDomainStatus() == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "btTaskAggregateSnapshot.domainStatus 不能为空");
        }
        DownloadTaskModel downloadTaskModel = loadTaskWithEngineTasksOrThrow(taskId);
        if (DownloadTaskStatus.CANCELLED.name().equals(downloadTaskModel.getDomainStatus())) {
            LOGGER.info("任务已删除，忽略 BT 聚合同步结果: taskId={}, primaryEngineGid={}",
                taskId, btTaskAggregateSnapshot.getPrimaryEngineGid());
            return new TaskOperationResult(downloadTaskModel, null, true);
        }

        long now = System.currentTimeMillis();
        DownloadTaskStatus fromStatus = DownloadTaskStatus.valueOf(downloadTaskModel.getDomainStatus());
        boolean changed = applyBtAggregate(downloadTaskModel, btTaskAggregateSnapshot, now);
        if (!changed) {
            return new TaskOperationResult(downloadTaskModel, null, true);
        }

        downloadTaskRepository.updateCoreSnapshot(taskModelConverter.toDownloadTaskDO(downloadTaskModel));
        downloadEngineTaskRepository.replaceByTaskId(
            taskId,
            downloadTaskModel.getEngineTasks().stream()
                .map(downloadEngineTaskModelConverter::toDO)
                .collect(java.util.stream.Collectors.toList())
        );

        TaskDomainEvent taskDomainEvent = null;
        if (fromStatus != btTaskAggregateSnapshot.getDomainStatus()) {
            taskDomainEvent = new TaskDomainEvent(
                downloadTaskModel.getId(),
                downloadTaskModel.getTaskCode(),
                fromStatus,
                btTaskAggregateSnapshot.getDomainStatus(),
                TaskTriggerType.ENGINE_SYNC,
                now
            );
            taskStateLogRepository.save(taskModelConverter.toTaskStateLogDO(
                downloadTaskModel,
                taskDomainEvent,
                "ENGINE",
                "aria2 轮询同步 BT 聚合快照"
            ));
        }
        LOGGER.info("同步 BT 聚合快照成功: taskId={}, primaryEngineGid={}, engineTaskSize={}, fileCount={}",
            downloadTaskModel.getId(),
            downloadTaskModel.getEngineGid(),
            downloadTaskModel.getEngineTasks().size(),
            downloadTaskModel.getTorrentFiles() == null ? 0 : downloadTaskModel.getTorrentFiles().size());
        return new TaskOperationResult(downloadTaskModel, taskDomainEvent, false);
    }

    /**
     * 更新 BT 文件列表元数据，不触发状态流转。
     *
     * @param taskId 任务 ID
     * @param torrentFiles BT 文件列表
     * @return 更新结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskOperationResult updateTorrentFiles(Long taskId, List<TorrentFileItem> torrentFiles) {
        DownloadTaskModel downloadTaskModel = loadTaskOrThrow(taskId);
        if (Objects.equals(downloadTaskModel.getTorrentFiles(), torrentFiles)) {
            return new TaskOperationResult(downloadTaskModel, null, true);
        }
        downloadTaskModel.setTorrentFiles(torrentFiles);
        downloadTaskModel.setTorrentFileListJson(null);
        downloadTaskModel.setUpdatedAt(System.currentTimeMillis());
        downloadTaskRepository.updateCoreSnapshot(taskModelConverter.toDownloadTaskDO(downloadTaskModel));
        LOGGER.info("更新 BT 文件列表成功: taskId={}, fileCount={}",
            taskId, torrentFiles == null ? 0 : torrentFiles.size());
        return new TaskOperationResult(downloadTaskModel, null, false);
    }

    /**
     * 绑定 aria2 新任务 gid，解决 magnet 元数据任务切换到真实下载任务后的跟踪问题。
     *
     * @param taskId 任务 ID
     * @param engineGid 最新引擎 gid
     * @return 更新结果
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskOperationResult rebindEngineGid(Long taskId, String engineGid) {
        if (!StringUtils.hasText(engineGid)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "engineGid 不能为空");
        }
        DownloadTaskModel downloadTaskModel = loadTaskOrThrow(taskId);
        String normalizedEngineGid = engineGid.trim();
        if (normalizedEngineGid.equals(downloadTaskModel.getEngineGid())) {
            return new TaskOperationResult(downloadTaskModel, null, true);
        }
        String oldEngineGid = downloadTaskModel.getEngineGid();
        downloadTaskModel.setEngineGid(normalizedEngineGid);
        downloadTaskModel.setUpdatedAt(System.currentTimeMillis());
        downloadTaskModel.setVersion(safeInt(downloadTaskModel.getVersion()) + 1);
        downloadTaskRepository.updateCoreSnapshot(taskModelConverter.toDownloadTaskDO(downloadTaskModel));
        LOGGER.info("重绑定 aria2 gid 成功: taskId={}, oldEngineGid={}, newEngineGid={}",
            taskId, oldEngineGid, normalizedEngineGid);
        return new TaskOperationResult(downloadTaskModel, null, false);
    }

    private TaskOperationResult applyTransition(
        DownloadTaskModel downloadTaskModel,
        TaskTriggerType triggerType,
        String triggerSource,
        String remark
    ) {
        TaskDomainEvent taskDomainEvent = taskStateMachineService.transit(
            downloadTaskModel,
            triggerType,
            System.currentTimeMillis()
        );
        persistTransition(downloadTaskModel, taskDomainEvent, triggerSource, remark);
        LOGGER.info("任务状态流转成功: taskId={}, triggerType={}, fromStatus={}, toStatus={}",
            downloadTaskModel.getId(),
            triggerType.name(),
            taskDomainEvent.getFromStatus(),
            taskDomainEvent.getToStatus());
        return new TaskOperationResult(downloadTaskModel, taskDomainEvent, false);
    }

    private DownloadTaskModel loadTaskOrThrow(Long taskId) {
        if (taskId == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "taskId 不能为空");
        }
        return downloadTaskRepository.findById(taskId)
            .map(taskModelConverter::toModel)
            .orElseThrow(() -> new BizException(ErrorCode.TASK_NOT_FOUND, "任务不存在: " + taskId));
    }

    private DownloadTaskModel loadTaskWithEngineTasksOrThrow(Long taskId) {
        DownloadTaskModel downloadTaskModel = loadTaskOrThrow(taskId);
        downloadTaskModel.setEngineTasks(downloadEngineTaskRepository.listByTaskId(taskId).stream()
            .map(downloadEngineTaskModelConverter::toModel)
            .collect(java.util.stream.Collectors.toList()));
        if (downloadTaskModel.getEngineTasks() == null) {
            downloadTaskModel.setEngineTasks(Collections.emptyList());
        }
        return downloadTaskModel;
    }

    /**
     * 在一个事务内同时更新任务快照与状态日志，保证领域状态变更可追踪。
     */
    private void persistTransition(
        DownloadTaskModel downloadTaskModel,
        TaskDomainEvent taskDomainEvent,
        String triggerSource,
        String remark
    ) {
        downloadTaskRepository.updateCoreSnapshot(taskModelConverter.toDownloadTaskDO(downloadTaskModel));
        taskStateLogRepository.save(taskModelConverter.toTaskStateLogDO(
            downloadTaskModel,
            taskDomainEvent,
            triggerSource,
            remark
        ));
    }

    private String resolveTriggerSource(String clientType, String fallback) {
        if (!StringUtils.hasText(clientType)) {
            return fallback;
        }
        return clientType.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 应用引擎快照字段，只有核心快照发生变化时才更新版本与更新时间。
     *
     * @param downloadTaskModel 当前任务
     * @param engineTaskSnapshot 引擎快照
     * @param targetStatus 同步后的领域状态
     * @param now 当前时间戳
     * @return 是否存在有效变更
     */
    private boolean applySnapshot(
        DownloadTaskModel downloadTaskModel,
        EngineTaskSnapshot engineTaskSnapshot,
        DownloadTaskStatus targetStatus,
        long now
    ) {
        boolean changed = false;
        changed |= updateString(downloadTaskModel.getDomainStatus(), targetStatus.name(), downloadTaskModel::setDomainStatus);
        changed |= updateString(
            downloadTaskModel.getEngineStatus(),
            normalizeBlank(engineTaskSnapshot.getEngineStatus()),
            downloadTaskModel::setEngineStatus
        );
        changed |= updateString(
            downloadTaskModel.getEngineGid(),
            normalizeBlank(engineTaskSnapshot.getEngineGid()),
            downloadTaskModel::setEngineGid
        );
        changed |= updateLong(
            downloadTaskModel.getTotalSizeBytes(),
            defaultLong(engineTaskSnapshot.getTotalSizeBytes()),
            downloadTaskModel::setTotalSizeBytes
        );
        changed |= updateLong(
            downloadTaskModel.getCompletedSizeBytes(),
            defaultLong(engineTaskSnapshot.getCompletedSizeBytes()),
            downloadTaskModel::setCompletedSizeBytes
        );
        changed |= updateLong(
            downloadTaskModel.getDownloadSpeedBps(),
            defaultLong(engineTaskSnapshot.getDownloadSpeedBps()),
            downloadTaskModel::setDownloadSpeedBps
        );
        changed |= updateLong(
            downloadTaskModel.getUploadSpeedBps(),
            defaultLong(engineTaskSnapshot.getUploadSpeedBps()),
            downloadTaskModel::setUploadSpeedBps
        );
        changed |= updateString(
            downloadTaskModel.getErrorCode(),
            normalizeBlank(engineTaskSnapshot.getErrorCode()),
            downloadTaskModel::setErrorCode
        );
        changed |= updateString(
            downloadTaskModel.getErrorMessage(),
            normalizeBlank(engineTaskSnapshot.getErrorMessage()),
            downloadTaskModel::setErrorMessage
        );
        if (!changed) {
            return false;
        }
        downloadTaskModel.setLastSyncAt(now);
        downloadTaskModel.setUpdatedAt(now);
        downloadTaskModel.setVersion(safeInt(downloadTaskModel.getVersion()) + 1);
        return true;
    }

    /**
     * 应用 BT 聚合快照，主任务只保留聚合摘要，子任务明细由独立表承载。
     *
     * @param downloadTaskModel 当前任务
     * @param btTaskAggregateSnapshot BT 聚合同步快照
     * @param now 当前时间戳
     * @return 是否存在有效变更
     */
    private boolean applyBtAggregate(
        DownloadTaskModel downloadTaskModel,
        BtTaskAggregateSnapshot btTaskAggregateSnapshot,
        long now
    ) {
        boolean changed = false;
        changed |= updateString(
            downloadTaskModel.getDomainStatus(),
            btTaskAggregateSnapshot.getDomainStatus().name(),
            downloadTaskModel::setDomainStatus
        );
        changed |= updateString(
            downloadTaskModel.getEngineStatus(),
            normalizeBlank(btTaskAggregateSnapshot.getEngineStatus()),
            downloadTaskModel::setEngineStatus
        );
        changed |= updateString(
            downloadTaskModel.getEngineGid(),
            normalizeBlank(btTaskAggregateSnapshot.getPrimaryEngineGid()),
            downloadTaskModel::setEngineGid
        );
        changed |= updateLong(
            downloadTaskModel.getTotalSizeBytes(),
            defaultLong(btTaskAggregateSnapshot.getTotalSizeBytes()),
            downloadTaskModel::setTotalSizeBytes
        );
        changed |= updateLong(
            downloadTaskModel.getCompletedSizeBytes(),
            defaultLong(btTaskAggregateSnapshot.getCompletedSizeBytes()),
            downloadTaskModel::setCompletedSizeBytes
        );
        changed |= updateLong(
            downloadTaskModel.getDownloadSpeedBps(),
            defaultLong(btTaskAggregateSnapshot.getDownloadSpeedBps()),
            downloadTaskModel::setDownloadSpeedBps
        );
        changed |= updateLong(
            downloadTaskModel.getUploadSpeedBps(),
            defaultLong(btTaskAggregateSnapshot.getUploadSpeedBps()),
            downloadTaskModel::setUploadSpeedBps
        );
        changed |= updateString(
            downloadTaskModel.getErrorCode(),
            normalizeBlank(btTaskAggregateSnapshot.getErrorCode()),
            downloadTaskModel::setErrorCode
        );
        changed |= updateString(
            downloadTaskModel.getErrorMessage(),
            normalizeBlank(btTaskAggregateSnapshot.getErrorMessage()),
            downloadTaskModel::setErrorMessage
        );

        List<TorrentFileItem> nextTorrentFiles = btTaskAggregateSnapshot.getTorrentFiles() == null
            ? Collections.emptyList()
            : btTaskAggregateSnapshot.getTorrentFiles();
        if (!Objects.equals(downloadTaskModel.getTorrentFiles(), nextTorrentFiles)) {
            downloadTaskModel.setTorrentFiles(nextTorrentFiles);
            downloadTaskModel.setTorrentFileListJson(null);
            changed = true;
        }

        List<DownloadEngineTaskModel> nextEngineTasks = btTaskAggregateSnapshot.getEngineTasks() == null
            ? Collections.emptyList()
            : btTaskAggregateSnapshot.getEngineTasks();
        if (!Objects.equals(downloadTaskModel.getEngineTasks(), nextEngineTasks)) {
            downloadTaskModel.setEngineTasks(nextEngineTasks);
            changed = true;
        }

        if (!changed) {
            return false;
        }
        downloadTaskModel.setLastSyncAt(now);
        downloadTaskModel.setUpdatedAt(now);
        downloadTaskModel.setVersion(safeInt(downloadTaskModel.getVersion()) + 1);
        return true;
    }

    private boolean updateString(String currentValue, String newValue, java.util.function.Consumer<String> consumer) {
        if (equalsNullable(currentValue, newValue)) {
            return false;
        }
        consumer.accept(newValue);
        return true;
    }

    private boolean updateLong(Long currentValue, Long newValue, java.util.function.Consumer<Long> consumer) {
        if (defaultLong(currentValue).equals(defaultLong(newValue))) {
            return false;
        }
        consumer.accept(defaultLong(newValue));
        return true;
    }

    private boolean equalsNullable(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
