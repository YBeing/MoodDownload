package com.mooddownload.local.service.task.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooddownload.local.mapper.task.DownloadEngineTaskDO;
import com.mooddownload.local.service.task.model.DownloadEngineTaskModel;
import com.mooddownload.local.service.task.model.TorrentFileItem;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 下载子任务模型转换器。
 */
@Component
public class DownloadEngineTaskModelConverter {

    private static final TypeReference<List<TorrentFileItem>> TORRENT_FILE_LIST_TYPE =
        new TypeReference<List<TorrentFileItem>>() { };

    private final ObjectMapper objectMapper;

    public DownloadEngineTaskModelConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将持久化对象转换为领域模型。
     *
     * @param downloadEngineTaskDO 持久化对象
     * @return 领域模型
     */
    public DownloadEngineTaskModel toModel(DownloadEngineTaskDO downloadEngineTaskDO) {
        if (downloadEngineTaskDO == null) {
            return null;
        }
        DownloadEngineTaskModel downloadEngineTaskModel = new DownloadEngineTaskModel();
        downloadEngineTaskModel.setId(downloadEngineTaskDO.getId());
        downloadEngineTaskModel.setTaskId(downloadEngineTaskDO.getTaskId());
        downloadEngineTaskModel.setEngineGid(downloadEngineTaskDO.getEngineGid());
        downloadEngineTaskModel.setParentEngineGid(downloadEngineTaskDO.getParentEngineGid());
        downloadEngineTaskModel.setEngineStatus(downloadEngineTaskDO.getEngineStatus());
        downloadEngineTaskModel.setTorrentFileListJson(downloadEngineTaskDO.getTorrentFileListJson());
        downloadEngineTaskModel.setTorrentFiles(parseTorrentFiles(downloadEngineTaskDO.getTorrentFileListJson()));
        downloadEngineTaskModel.setMetadataOnly(downloadEngineTaskDO.getMetadataOnly() != null
            && downloadEngineTaskDO.getMetadataOnly() == 1);
        downloadEngineTaskModel.setTotalSizeBytes(downloadEngineTaskDO.getTotalSizeBytes());
        downloadEngineTaskModel.setCompletedSizeBytes(downloadEngineTaskDO.getCompletedSizeBytes());
        downloadEngineTaskModel.setDownloadSpeedBps(downloadEngineTaskDO.getDownloadSpeedBps());
        downloadEngineTaskModel.setUploadSpeedBps(downloadEngineTaskDO.getUploadSpeedBps());
        downloadEngineTaskModel.setErrorCode(downloadEngineTaskDO.getErrorCode());
        downloadEngineTaskModel.setErrorMessage(downloadEngineTaskDO.getErrorMessage());
        downloadEngineTaskModel.setCreatedAt(downloadEngineTaskDO.getCreatedAt());
        downloadEngineTaskModel.setUpdatedAt(downloadEngineTaskDO.getUpdatedAt());
        return downloadEngineTaskModel;
    }

    /**
     * 将领域模型转换为持久化对象。
     *
     * @param downloadEngineTaskModel 领域模型
     * @return 持久化对象
     */
    public DownloadEngineTaskDO toDO(DownloadEngineTaskModel downloadEngineTaskModel) {
        DownloadEngineTaskDO downloadEngineTaskDO = new DownloadEngineTaskDO();
        downloadEngineTaskDO.setId(downloadEngineTaskModel.getId());
        downloadEngineTaskDO.setTaskId(downloadEngineTaskModel.getTaskId());
        downloadEngineTaskDO.setEngineGid(downloadEngineTaskModel.getEngineGid());
        downloadEngineTaskDO.setParentEngineGid(downloadEngineTaskModel.getParentEngineGid());
        downloadEngineTaskDO.setEngineStatus(downloadEngineTaskModel.getEngineStatus());
        downloadEngineTaskDO.setTorrentFileListJson(resolveTorrentFileListJson(downloadEngineTaskModel));
        downloadEngineTaskDO.setMetadataOnly(Boolean.TRUE.equals(downloadEngineTaskModel.getMetadataOnly()) ? 1 : 0);
        downloadEngineTaskDO.setTotalSizeBytes(downloadEngineTaskModel.getTotalSizeBytes());
        downloadEngineTaskDO.setCompletedSizeBytes(downloadEngineTaskModel.getCompletedSizeBytes());
        downloadEngineTaskDO.setDownloadSpeedBps(downloadEngineTaskModel.getDownloadSpeedBps());
        downloadEngineTaskDO.setUploadSpeedBps(downloadEngineTaskModel.getUploadSpeedBps());
        downloadEngineTaskDO.setErrorCode(downloadEngineTaskModel.getErrorCode());
        downloadEngineTaskDO.setErrorMessage(downloadEngineTaskModel.getErrorMessage());
        downloadEngineTaskDO.setCreatedAt(downloadEngineTaskModel.getCreatedAt());
        downloadEngineTaskDO.setUpdatedAt(downloadEngineTaskModel.getUpdatedAt());
        return downloadEngineTaskDO;
    }

    private List<TorrentFileItem> parseTorrentFiles(String torrentFileListJson) {
        if (!StringUtils.hasText(torrentFileListJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(torrentFileListJson, TORRENT_FILE_LIST_TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("解析下载子任务文件列表失败", exception);
        }
    }

    private String resolveTorrentFileListJson(DownloadEngineTaskModel downloadEngineTaskModel) {
        if (StringUtils.hasText(downloadEngineTaskModel.getTorrentFileListJson())) {
            return downloadEngineTaskModel.getTorrentFileListJson();
        }
        if (downloadEngineTaskModel.getTorrentFiles() == null || downloadEngineTaskModel.getTorrentFiles().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(downloadEngineTaskModel.getTorrentFiles());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化下载子任务文件列表失败", exception);
        }
    }
}
