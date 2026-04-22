package com.mooddownload.local.service.task.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooddownload.local.mapper.task.DownloadTaskDO;
import com.mooddownload.local.mapper.task.TaskStateLogDO;
import com.mooddownload.local.service.task.event.TaskDomainEvent;
import com.mooddownload.local.service.task.model.DownloadTaskModel;
import com.mooddownload.local.service.task.model.TorrentFileItem;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 任务领域模型转换器。
 */
@Component
public class TaskModelConverter {

    private static final TypeReference<List<TorrentFileItem>> TORRENT_FILE_LIST_TYPE =
        new TypeReference<List<TorrentFileItem>>() { };

    private static final String DEFAULT_ENTRY_TYPE = "MANUAL";

    private static final String DEFAULT_SOURCE_PROVIDER = "GENERIC";

    private static final String DEFAULT_ENGINE_PROFILE_CODE = "default";

    private final ObjectMapper objectMapper;

    public TaskModelConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将持久化对象转换为领域模型。
     *
     * @param downloadTaskDO 持久化对象
     * @return 领域模型
     */
    public DownloadTaskModel toModel(DownloadTaskDO downloadTaskDO) {
        if (downloadTaskDO == null) {
            return null;
        }
        DownloadTaskModel downloadTaskModel = new DownloadTaskModel();
        downloadTaskModel.setId(downloadTaskDO.getId());
        downloadTaskModel.setTaskCode(downloadTaskDO.getTaskCode());
        downloadTaskModel.setSourceType(downloadTaskDO.getSourceType());
        downloadTaskModel.setSourceUri(downloadTaskDO.getSourceUri());
        downloadTaskModel.setSourceHash(downloadTaskDO.getSourceHash());
        downloadTaskModel.setTorrentFilePath(downloadTaskDO.getTorrentFilePath());
        downloadTaskModel.setTorrentFileListJson(downloadTaskDO.getTorrentFileListJson());
        downloadTaskModel.setTorrentFiles(parseTorrentFiles(downloadTaskDO.getTorrentFileListJson()));
        downloadTaskModel.setEngineTasks(Collections.emptyList());
        downloadTaskModel.setDisplayName(downloadTaskDO.getDisplayName());
        downloadTaskModel.setDomainStatus(downloadTaskDO.getDomainStatus());
        downloadTaskModel.setEngineStatus(downloadTaskDO.getEngineStatus());
        downloadTaskModel.setEngineGid(downloadTaskDO.getEngineGid());
        downloadTaskModel.setQueuePriority(downloadTaskDO.getQueuePriority());
        downloadTaskModel.setSaveDir(downloadTaskDO.getSaveDir());
        downloadTaskModel.setTotalSizeBytes(downloadTaskDO.getTotalSizeBytes());
        downloadTaskModel.setCompletedSizeBytes(downloadTaskDO.getCompletedSizeBytes());
        downloadTaskModel.setDownloadSpeedBps(downloadTaskDO.getDownloadSpeedBps());
        downloadTaskModel.setUploadSpeedBps(downloadTaskDO.getUploadSpeedBps());
        downloadTaskModel.setErrorCode(downloadTaskDO.getErrorCode());
        downloadTaskModel.setErrorMessage(downloadTaskDO.getErrorMessage());
        downloadTaskModel.setRetryCount(downloadTaskDO.getRetryCount());
        downloadTaskModel.setMaxRetryCount(downloadTaskDO.getMaxRetryCount());
        downloadTaskModel.setClientRequestId(downloadTaskDO.getClientRequestId());
        downloadTaskModel.setEntryType(resolveText(downloadTaskDO.getEntryType(), DEFAULT_ENTRY_TYPE));
        downloadTaskModel.setSourceProvider(resolveText(downloadTaskDO.getSourceProvider(), DEFAULT_SOURCE_PROVIDER));
        downloadTaskModel.setSourceSiteHost(downloadTaskDO.getSourceSiteHost());
        downloadTaskModel.setEntryContextJson(downloadTaskDO.getEntryContextJson());
        downloadTaskModel.setEngineProfileCode(resolveText(
            downloadTaskDO.getEngineProfileCode(),
            DEFAULT_ENGINE_PROFILE_CODE
        ));
        downloadTaskModel.setOpenFolderPath(downloadTaskDO.getOpenFolderPath());
        downloadTaskModel.setPrimaryFilePath(downloadTaskDO.getPrimaryFilePath());
        downloadTaskModel.setCompletedAt(downloadTaskDO.getCompletedAt());
        downloadTaskModel.setLastSyncAt(downloadTaskDO.getLastSyncAt());
        downloadTaskModel.setVersion(downloadTaskDO.getVersion());
        downloadTaskModel.setCreatedAt(downloadTaskDO.getCreatedAt());
        downloadTaskModel.setUpdatedAt(downloadTaskDO.getUpdatedAt());
        return downloadTaskModel;
    }

    /**
     * 将领域模型转换为持久化对象。
     *
     * @param downloadTaskModel 领域模型
     * @return 持久化对象
     */
    public DownloadTaskDO toDownloadTaskDO(DownloadTaskModel downloadTaskModel) {
        DownloadTaskDO downloadTaskDO = new DownloadTaskDO();
        downloadTaskDO.setId(downloadTaskModel.getId());
        downloadTaskDO.setTaskCode(downloadTaskModel.getTaskCode());
        downloadTaskDO.setSourceType(downloadTaskModel.getSourceType());
        downloadTaskDO.setSourceUri(downloadTaskModel.getSourceUri());
        downloadTaskDO.setSourceHash(downloadTaskModel.getSourceHash());
        downloadTaskDO.setTorrentFilePath(downloadTaskModel.getTorrentFilePath());
        downloadTaskDO.setTorrentFileListJson(resolveTorrentFileListJson(downloadTaskModel));
        downloadTaskDO.setDisplayName(downloadTaskModel.getDisplayName());
        downloadTaskDO.setDomainStatus(downloadTaskModel.getDomainStatus());
        downloadTaskDO.setEngineStatus(downloadTaskModel.getEngineStatus());
        downloadTaskDO.setEngineGid(downloadTaskModel.getEngineGid());
        downloadTaskDO.setQueuePriority(downloadTaskModel.getQueuePriority());
        downloadTaskDO.setSaveDir(downloadTaskModel.getSaveDir());
        downloadTaskDO.setTotalSizeBytes(downloadTaskModel.getTotalSizeBytes());
        downloadTaskDO.setCompletedSizeBytes(downloadTaskModel.getCompletedSizeBytes());
        downloadTaskDO.setDownloadSpeedBps(downloadTaskModel.getDownloadSpeedBps());
        downloadTaskDO.setUploadSpeedBps(downloadTaskModel.getUploadSpeedBps());
        downloadTaskDO.setErrorCode(downloadTaskModel.getErrorCode());
        downloadTaskDO.setErrorMessage(downloadTaskModel.getErrorMessage());
        downloadTaskDO.setRetryCount(downloadTaskModel.getRetryCount());
        downloadTaskDO.setMaxRetryCount(downloadTaskModel.getMaxRetryCount());
        downloadTaskDO.setClientRequestId(downloadTaskModel.getClientRequestId());
        downloadTaskDO.setEntryType(resolveText(downloadTaskModel.getEntryType(), DEFAULT_ENTRY_TYPE));
        downloadTaskDO.setSourceProvider(resolveText(downloadTaskModel.getSourceProvider(), DEFAULT_SOURCE_PROVIDER));
        downloadTaskDO.setSourceSiteHost(downloadTaskModel.getSourceSiteHost());
        downloadTaskDO.setEntryContextJson(downloadTaskModel.getEntryContextJson());
        downloadTaskDO.setEngineProfileCode(resolveText(
            downloadTaskModel.getEngineProfileCode(),
            DEFAULT_ENGINE_PROFILE_CODE
        ));
        downloadTaskDO.setOpenFolderPath(downloadTaskModel.getOpenFolderPath());
        downloadTaskDO.setPrimaryFilePath(downloadTaskModel.getPrimaryFilePath());
        downloadTaskDO.setCompletedAt(downloadTaskModel.getCompletedAt());
        downloadTaskDO.setLastSyncAt(downloadTaskModel.getLastSyncAt());
        downloadTaskDO.setVersion(downloadTaskModel.getVersion());
        downloadTaskDO.setCreatedAt(downloadTaskModel.getCreatedAt());
        downloadTaskDO.setUpdatedAt(downloadTaskModel.getUpdatedAt());
        return downloadTaskDO;
    }

    /**
     * 将领域事件转换为状态日志持久化对象。
     *
     * @param taskModel 当前任务快照
     * @param taskDomainEvent 领域事件
     * @param triggerSource 触发来源
     * @param remark 备注
     * @return 状态日志持久化对象
     */
    public TaskStateLogDO toTaskStateLogDO(
        DownloadTaskModel taskModel,
        TaskDomainEvent taskDomainEvent,
        String triggerSource,
        String remark
    ) {
        TaskStateLogDO taskStateLogDO = new TaskStateLogDO();
        taskStateLogDO.setTaskId(taskModel.getId());
        taskStateLogDO.setFromStatus(taskDomainEvent.getFromStatus() == null ? null : taskDomainEvent.getFromStatus().name());
        taskStateLogDO.setToStatus(taskDomainEvent.getToStatus().name());
        taskStateLogDO.setTriggerSource(triggerSource);
        taskStateLogDO.setTriggerType(taskDomainEvent.getTriggerType().name());
        taskStateLogDO.setRemark(remark);
        taskStateLogDO.setCreatedAt(taskDomainEvent.getOccurredAt());
        return taskStateLogDO;
    }

    private List<TorrentFileItem> parseTorrentFiles(String torrentFileListJson) {
        if (!StringUtils.hasText(torrentFileListJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(torrentFileListJson, TORRENT_FILE_LIST_TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("解析 BT 文件列表失败", exception);
        }
    }

    private String resolveTorrentFileListJson(DownloadTaskModel downloadTaskModel) {
        if (StringUtils.hasText(downloadTaskModel.getTorrentFileListJson())) {
            return downloadTaskModel.getTorrentFileListJson();
        }
        if (downloadTaskModel.getTorrentFiles() == null || downloadTaskModel.getTorrentFiles().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(downloadTaskModel.getTorrentFiles());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化 BT 文件列表失败", exception);
        }
    }

    private String resolveText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
