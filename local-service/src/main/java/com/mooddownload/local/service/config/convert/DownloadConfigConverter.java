package com.mooddownload.local.service.config.convert;

import com.mooddownload.local.mapper.config.DownloadConfigDO;
import com.mooddownload.local.service.config.model.DownloadConfigModel;
import org.springframework.stereotype.Component;

/**
 * 下载配置模型转换器。
 */
@Component
public class DownloadConfigConverter {

    /**
     * 将持久化对象转换为领域模型。
     *
     * @param downloadConfigDO 持久化对象
     * @return 领域模型
     */
    public DownloadConfigModel toModel(DownloadConfigDO downloadConfigDO) {
        if (downloadConfigDO == null) {
            return null;
        }
        DownloadConfigModel downloadConfigModel = new DownloadConfigModel();
        downloadConfigModel.setDefaultSaveDir(downloadConfigDO.getDefaultSaveDir());
        downloadConfigModel.setMaxConcurrentDownloads(downloadConfigDO.getMaxConcurrentDownloads());
        downloadConfigModel.setMaxGlobalDownloadSpeed(downloadConfigDO.getMaxGlobalDownloadSpeed());
        downloadConfigModel.setMaxGlobalUploadSpeed(downloadConfigDO.getMaxGlobalUploadSpeed());
        downloadConfigModel.setBrowserCaptureEnabled(toBoolean(downloadConfigDO.getBrowserCaptureEnabled()));
        downloadConfigModel.setClipboardMonitorEnabled(toBoolean(downloadConfigDO.getClipboardMonitorEnabled()));
        downloadConfigModel.setAutoStartEnabled(toBoolean(downloadConfigDO.getAutoStartEnabled()));
        downloadConfigModel.setUpdatedAt(downloadConfigDO.getUpdatedAt());
        return downloadConfigModel;
    }

    /**
     * 将持久化对象拷贝为可更新对象。
     *
     * @param source 原始配置
     * @return 拷贝后的持久化对象
     */
    public DownloadConfigDO copy(DownloadConfigDO source) {
        DownloadConfigDO target = new DownloadConfigDO();
        target.setId(source.getId());
        target.setDefaultSaveDir(source.getDefaultSaveDir());
        target.setMaxConcurrentDownloads(source.getMaxConcurrentDownloads());
        target.setMaxGlobalDownloadSpeed(source.getMaxGlobalDownloadSpeed());
        target.setMaxGlobalUploadSpeed(source.getMaxGlobalUploadSpeed());
        target.setBrowserCaptureEnabled(source.getBrowserCaptureEnabled());
        target.setClipboardMonitorEnabled(source.getClipboardMonitorEnabled());
        target.setAutoStartEnabled(source.getAutoStartEnabled());
        target.setLocalApiToken(source.getLocalApiToken());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    /**
     * 将布尔开关转换为持久化整型值。
     *
     * @param value 布尔值
     * @return 整型值
     */
    public Integer toInt(Boolean value) {
        return value == null ? null : (value ? 1 : 0);
    }

    private Boolean toBoolean(Integer value) {
        return value != null && value == 1;
    }
}
