package com.mooddownload.local.service.task;

import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.service.task.event.TaskDomainEvent;
import com.mooddownload.local.service.task.model.CreateTaskCommand;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.state.DownloadTaskStatus;
import com.mooddownload.local.service.task.state.TaskSourceType;
import com.mooddownload.local.service.task.state.TaskTriggerType;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 任务状态机服务，负责任务初始化与领域状态流转。
 */
@Service
public class TaskStateMachineService {

    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private static final int DEFAULT_QUEUE_PRIORITY = 100;

    private static final String DEFAULT_ENTRY_TYPE = "MANUAL";

    private static final String DEFAULT_SOURCE_PROVIDER = "GENERIC";

    private static final String DEFAULT_ENGINE_PROFILE_CODE = "default";

    /**
     * 初始化任务聚合根。
     *
     * @param command 创建任务命令
     * @param now 当前时间戳
     * @return 初始化后的任务模型
     */
    public DownloadTaskModel initializeTask(CreateTaskCommand command, long now) {
        validateCreateCommand(command);
        TaskSourceType taskSourceType = resolveSourceType(command.getSourceType());
        DownloadTaskModel downloadTaskModel = new DownloadTaskModel();
        downloadTaskModel.setTaskCode(generateTaskCode(now));
        downloadTaskModel.setSourceType(taskSourceType.name());
        downloadTaskModel.setSourceUri(resolveSourceUri(command, taskSourceType));
        downloadTaskModel.setSourceHash(normalizeBlank(command.getSourceHash()));
        downloadTaskModel.setTorrentFilePath(normalizeBlank(command.getTorrentFilePath()));
        downloadTaskModel.setDisplayName(resolveDisplayName(command));
        downloadTaskModel.setDomainStatus(DownloadTaskStatus.PENDING.name());
        downloadTaskModel.setEngineStatus("UNKNOWN");
        downloadTaskModel.setQueuePriority(DEFAULT_QUEUE_PRIORITY);
        downloadTaskModel.setSaveDir(command.getSaveDir().trim());
        downloadTaskModel.setTotalSizeBytes(0L);
        downloadTaskModel.setCompletedSizeBytes(0L);
        downloadTaskModel.setDownloadSpeedBps(0L);
        downloadTaskModel.setUploadSpeedBps(0L);
        downloadTaskModel.setRetryCount(0);
        downloadTaskModel.setMaxRetryCount(resolveMaxRetryCount(command.getMaxRetryCount()));
        downloadTaskModel.setClientRequestId(command.getClientRequestId().trim());
        downloadTaskModel.setEntryType(resolveOrDefault(command.getEntryType(), DEFAULT_ENTRY_TYPE));
        downloadTaskModel.setSourceProvider(resolveOrDefault(command.getSourceProvider(), DEFAULT_SOURCE_PROVIDER));
        downloadTaskModel.setSourceSiteHost(normalizeBlank(command.getSourceSiteHost()));
        downloadTaskModel.setEntryContextJson(normalizeBlank(command.getEntryContextJson()));
        downloadTaskModel.setEngineProfileCode(resolveOrDefault(
            command.getEngineProfileCode(),
            DEFAULT_ENGINE_PROFILE_CODE
        ));
        downloadTaskModel.setOpenFolderPath(null);
        downloadTaskModel.setPrimaryFilePath(null);
        downloadTaskModel.setCompletedAt(null);
        downloadTaskModel.setVersion(0);
        downloadTaskModel.setCreatedAt(now);
        downloadTaskModel.setUpdatedAt(now);
        return downloadTaskModel;
    }

    /**
     * 按触发器推进任务状态。
     *
     * @param taskModel 当前任务模型
     * @param triggerType 触发器
     * @param now 当前时间戳
     * @return 领域事件
     */
    public TaskDomainEvent transit(DownloadTaskModel taskModel, TaskTriggerType triggerType, long now) {
        if (taskModel == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "任务模型不能为空");
        }
        if (triggerType == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "任务触发器不能为空");
        }
        DownloadTaskStatus fromStatus = resolveStatus(taskModel.getDomainStatus());
        DownloadTaskStatus toStatus = resolveTargetStatus(taskModel, fromStatus, triggerType);
        applyTransition(taskModel, triggerType, toStatus, now);
        return new TaskDomainEvent(
            taskModel.getId(),
            taskModel.getTaskCode(),
            fromStatus,
            toStatus,
            triggerType,
            now
        );
    }

    private void validateCreateCommand(CreateTaskCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "创建任务命令不能为空");
        }
        if (!StringUtils.hasText(command.getClientRequestId())) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "clientRequestId 不能为空");
        }
        if (!StringUtils.hasText(command.getSaveDir())) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "saveDir 不能为空");
        }
        TaskSourceType taskSourceType = resolveSourceType(command.getSourceType());
        if (taskSourceType == TaskSourceType.TORRENT) {
            if (!StringUtils.hasText(command.getTorrentFilePath())) {
                throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "torrentFilePath 不能为空");
            }
            return;
        }
        if (!StringUtils.hasText(command.getSourceUri())) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "sourceUri 不能为空");
        }
    }

    private TaskSourceType resolveSourceType(String sourceType) {
        TaskSourceType taskSourceType = TaskSourceType.fromCode(sourceType);
        if (taskSourceType == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "不支持的 sourceType: " + sourceType);
        }
        return taskSourceType;
    }

    private DownloadTaskStatus resolveStatus(String statusCode) {
        if (!StringUtils.hasText(statusCode)) {
            throw new BizException(ErrorCode.TASK_STATE_INVALID, "任务状态缺失");
        }
        try {
            return DownloadTaskStatus.valueOf(statusCode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.TASK_STATE_INVALID, "未知任务状态: " + statusCode);
        }
    }

    private DownloadTaskStatus resolveTargetStatus(
        DownloadTaskModel taskModel,
        DownloadTaskStatus fromStatus,
        TaskTriggerType triggerType
    ) {
        switch (triggerType) {
            case DISPATCH:
                if (fromStatus == DownloadTaskStatus.PENDING) {
                    return DownloadTaskStatus.DISPATCHING;
                }
                break;
            case DISPATCH_SUCCESS:
                if (fromStatus == DownloadTaskStatus.DISPATCHING) {
                    return DownloadTaskStatus.RUNNING;
                }
                break;
            case DISPATCH_FAILED_RETRY:
                if (fromStatus == DownloadTaskStatus.DISPATCHING) {
                    return DownloadTaskStatus.PENDING;
                }
                break;
            case DISPATCH_FAILED_FINAL:
                if (fromStatus == DownloadTaskStatus.DISPATCHING) {
                    return DownloadTaskStatus.FAILED;
                }
                break;
            case PAUSE:
                if (fromStatus == DownloadTaskStatus.RUNNING || fromStatus == DownloadTaskStatus.PENDING) {
                    return DownloadTaskStatus.PAUSED;
                }
                break;
            case RESUME:
                if (fromStatus == DownloadTaskStatus.PAUSED || fromStatus == DownloadTaskStatus.FAILED) {
                    return DownloadTaskStatus.PENDING;
                }
                break;
            case CANCEL:
                if (fromStatus == DownloadTaskStatus.PENDING
                    || fromStatus == DownloadTaskStatus.DISPATCHING
                    || fromStatus == DownloadTaskStatus.PAUSED
                    || fromStatus == DownloadTaskStatus.FAILED
                    || fromStatus == DownloadTaskStatus.RUNNING
                    || fromStatus == DownloadTaskStatus.COMPLETED) {
                    return DownloadTaskStatus.CANCELLED;
                }
                break;
            case RETRY:
                if (fromStatus != DownloadTaskStatus.FAILED) {
                    throw new BizException(
                        ErrorCode.TASK_RETRY_NOT_ALLOWED,
                        "当前状态不允许重试: " + fromStatus.name()
                    );
                }
                if (safeInt(taskModel.getRetryCount()) >= safeInt(taskModel.getMaxRetryCount())) {
                    throw new BizException(
                        ErrorCode.TASK_MAX_RETRY_EXCEEDED,
                        "已达到最大重试次数: " + safeInt(taskModel.getMaxRetryCount())
                    );
                }
                return DownloadTaskStatus.PENDING;
            default:
                break;
        }
        throw new BizException(
            ErrorCode.TASK_STATE_INVALID,
            "状态流转不允许: " + fromStatus.name() + " -> " + triggerType.name()
        );
    }

    private void applyTransition(
        DownloadTaskModel taskModel,
        TaskTriggerType triggerType,
        DownloadTaskStatus toStatus,
        long now
    ) {
        taskModel.setDomainStatus(toStatus.name());
        taskModel.setUpdatedAt(now);
        taskModel.setVersion(safeInt(taskModel.getVersion()) + 1);
        switch (triggerType) {
            case DISPATCH_SUCCESS:
                taskModel.setEngineStatus("ACTIVE");
                taskModel.setErrorCode(null);
                taskModel.setErrorMessage(null);
                break;
            case DISPATCH_FAILED_FINAL:
                taskModel.setEngineStatus("ERROR");
                break;
            case PAUSE:
                taskModel.setEngineStatus("PAUSED");
                break;
            case CANCEL:
                taskModel.setEngineStatus("REMOVED");
                break;
            case DISPATCH:
                taskModel.setEngineStatus("SUBMITTING");
                taskModel.setErrorCode(null);
                taskModel.setErrorMessage(null);
                break;
            case DISPATCH_FAILED_RETRY:
                taskModel.setEngineStatus("UNKNOWN");
                break;
            case RESUME:
            case RETRY:
                taskModel.setEngineStatus("UNKNOWN");
                taskModel.setErrorCode(null);
                taskModel.setErrorMessage(null);
                break;
            default:
                break;
        }
        if (triggerType == TaskTriggerType.RETRY) {
            taskModel.setRetryCount(safeInt(taskModel.getRetryCount()) + 1);
        }
    }

    private String resolveSourceUri(CreateTaskCommand command, TaskSourceType taskSourceType) {
        if (taskSourceType == TaskSourceType.TORRENT && !StringUtils.hasText(command.getSourceUri())) {
            return command.getTorrentFilePath().trim();
        }
        return command.getSourceUri().trim();
    }

    private String resolveDisplayName(CreateTaskCommand command) {
        if (StringUtils.hasText(command.getDisplayName())) {
            return command.getDisplayName().trim();
        }
        String candidate = StringUtils.hasText(command.getTorrentFilePath())
            ? command.getTorrentFilePath().trim()
            : command.getSourceUri();
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        int slashIndex = Math.max(candidate.lastIndexOf('/'), candidate.lastIndexOf('\\'));
        if (slashIndex >= 0 && slashIndex < candidate.length() - 1) {
            return candidate.substring(slashIndex + 1);
        }
        return candidate;
    }

    private Integer resolveMaxRetryCount(Integer maxRetryCount) {
        int resolvedMaxRetryCount = maxRetryCount == null ? DEFAULT_MAX_RETRY_COUNT : maxRetryCount;
        if (resolvedMaxRetryCount <= 0) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "maxRetryCount 必须大于 0");
        }
        return resolvedMaxRetryCount;
    }

    private String generateTaskCode(long now) {
        return "TASK-" + now + "-" + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
