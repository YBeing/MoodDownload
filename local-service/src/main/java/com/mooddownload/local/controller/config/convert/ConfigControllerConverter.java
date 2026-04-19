package com.mooddownload.local.controller.config.convert;

import com.mooddownload.local.controller.config.vo.DownloadConfigResponse;
import com.mooddownload.local.controller.config.vo.UpdateDownloadConfigRequest;
import com.mooddownload.local.service.config.model.DownloadConfigModel;
import com.mooddownload.local.service.config.model.UpdateDownloadConfigCommand;
import org.springframework.stereotype.Component;

/**
 * 配置接口模型转换器。
 */
@Component
public class ConfigControllerConverter {

    /**
     * 将更新请求转换为更新命令。
     *
     * @param request 更新请求
     * @return 更新命令
     */
    public UpdateDownloadConfigCommand toUpdateCommand(UpdateDownloadConfigRequest request) {
        UpdateDownloadConfigCommand updateDownloadConfigCommand = new UpdateDownloadConfigCommand();
        updateDownloadConfigCommand.setDefaultSaveDir(request.getDefaultSaveDir());
        updateDownloadConfigCommand.setMaxConcurrentDownloads(request.getMaxConcurrentDownloads());
        updateDownloadConfigCommand.setMaxGlobalDownloadSpeed(request.getMaxGlobalDownloadSpeed());
        updateDownloadConfigCommand.setMaxGlobalUploadSpeed(request.getMaxGlobalUploadSpeed());
        updateDownloadConfigCommand.setBrowserCaptureEnabled(request.getBrowserCaptureEnabled());
        updateDownloadConfigCommand.setClipboardMonitorEnabled(request.getClipboardMonitorEnabled());
        return updateDownloadConfigCommand;
    }

    /**
     * 将配置模型转换为接口响应。
     *
     * @param downloadConfigModel 配置模型
     * @return 接口响应
     */
    public DownloadConfigResponse toResponse(DownloadConfigModel downloadConfigModel) {
        DownloadConfigResponse downloadConfigResponse = new DownloadConfigResponse();
        downloadConfigResponse.setDefaultSaveDir(downloadConfigModel.getDefaultSaveDir());
        downloadConfigResponse.setMaxConcurrentDownloads(downloadConfigModel.getMaxConcurrentDownloads());
        downloadConfigResponse.setMaxGlobalDownloadSpeed(downloadConfigModel.getMaxGlobalDownloadSpeed());
        downloadConfigResponse.setMaxGlobalUploadSpeed(downloadConfigModel.getMaxGlobalUploadSpeed());
        downloadConfigResponse.setBrowserCaptureEnabled(downloadConfigModel.getBrowserCaptureEnabled());
        downloadConfigResponse.setClipboardMonitorEnabled(downloadConfigModel.getClipboardMonitorEnabled());
        downloadConfigResponse.setAutoStartEnabled(downloadConfigModel.getAutoStartEnabled());
        downloadConfigResponse.setUpdatedAt(downloadConfigModel.getUpdatedAt());
        return downloadConfigResponse;
    }
}
