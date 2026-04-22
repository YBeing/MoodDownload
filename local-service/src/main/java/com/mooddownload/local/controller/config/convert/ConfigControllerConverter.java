package com.mooddownload.local.controller.config.convert;

import com.mooddownload.local.controller.config.vo.DownloadConfigResponse;
import com.mooddownload.local.controller.config.vo.BtTrackerSetResponse;
import com.mooddownload.local.controller.config.vo.EngineRuntimeApplyResponse;
import com.mooddownload.local.controller.config.vo.EngineRuntimeProfileItemVO;
import com.mooddownload.local.controller.config.vo.EngineRuntimeProfileResponse;
import com.mooddownload.local.controller.config.vo.SourceSiteRuleResponse;
import com.mooddownload.local.controller.config.vo.UpdateBtTrackerSetRequest;
import com.mooddownload.local.controller.config.vo.UpdateDownloadConfigRequest;
import com.mooddownload.local.controller.config.vo.UpdateEngineRuntimeProfileRequest;
import com.mooddownload.local.controller.config.vo.UpdateSourceSiteRuleRequest;
import com.mooddownload.local.service.config.model.BtTrackerSetModel;
import com.mooddownload.local.service.config.model.DownloadConfigModel;
import com.mooddownload.local.service.config.model.EngineRuntimeApplyResultModel;
import com.mooddownload.local.service.config.model.EngineRuntimeProfileItemModel;
import com.mooddownload.local.service.config.model.EngineRuntimeSnapshotModel;
import com.mooddownload.local.service.config.model.SourceSiteRuleModel;
import com.mooddownload.local.service.config.model.UpdateBtTrackerSetCommand;
import com.mooddownload.local.service.config.model.UpdateDownloadConfigCommand;
import com.mooddownload.local.service.config.model.UpdateEngineRuntimeProfileCommand;
import com.mooddownload.local.service.config.model.UpdateSourceSiteRuleCommand;
import java.util.ArrayList;
import java.util.List;
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
        updateDownloadConfigCommand.setActiveEngineProfileCode(request.getActiveEngineProfileCode());
        updateDownloadConfigCommand.setDeleteToRecycleBinEnabled(request.getDeleteToRecycleBinEnabled());
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
        downloadConfigResponse.setActiveEngineProfileCode(downloadConfigModel.getActiveEngineProfileCode());
        downloadConfigResponse.setDeleteToRecycleBinEnabled(downloadConfigModel.getDeleteToRecycleBinEnabled());
        downloadConfigResponse.setUpdatedAt(downloadConfigModel.getUpdatedAt());
        return downloadConfigResponse;
    }

    /**
     * 将引擎模板更新请求转换为命令。
     *
     * @param request 更新请求
     * @return 更新命令
     */
    public UpdateEngineRuntimeProfileCommand toUpdateEngineRuntimeProfileCommand(
        UpdateEngineRuntimeProfileRequest request
    ) {
        UpdateEngineRuntimeProfileCommand command = new UpdateEngineRuntimeProfileCommand();
        command.setProfileCode(request.getProfileCode());
        command.setProfileJson(request.getProfileJson());
        return command;
    }

    /**
     * 将引擎配置中心快照转换为接口响应。
     *
     * @param snapshotModel 快照模型
     * @return 接口响应
     */
    public EngineRuntimeProfileResponse toEngineRuntimeProfileResponse(EngineRuntimeSnapshotModel snapshotModel) {
        EngineRuntimeProfileResponse response = new EngineRuntimeProfileResponse();
        response.setActiveProfileCode(snapshotModel.getActiveProfileCode());
        response.setApplyStatus(snapshotModel.getApplyStatus());
        List<EngineRuntimeProfileItemVO> profileItemVOList = new ArrayList<EngineRuntimeProfileItemVO>();
        if (snapshotModel.getProfiles() != null) {
            for (EngineRuntimeProfileItemModel itemModel : snapshotModel.getProfiles()) {
                EngineRuntimeProfileItemVO itemVO = new EngineRuntimeProfileItemVO();
                itemVO.setProfileCode(itemModel.getProfileCode());
                itemVO.setProfileName(itemModel.getProfileName());
                itemVO.setTrackerSetCode(itemModel.getTrackerSetCode());
                itemVO.setProfileJson(itemModel.getProfileJson());
                itemVO.setIsDefault(itemModel.getIsDefault());
                itemVO.setEnabled(itemModel.getEnabled());
                profileItemVOList.add(itemVO);
            }
        }
        response.setProfiles(profileItemVOList);
        return response;
    }

    /**
     * 将引擎配置应用结果转换为接口响应。
     *
     * @param resultModel 应用结果
     * @return 接口响应
     */
    public EngineRuntimeApplyResponse toEngineRuntimeApplyResponse(EngineRuntimeApplyResultModel resultModel) {
        EngineRuntimeApplyResponse response = new EngineRuntimeApplyResponse();
        response.setProfileCode(resultModel.getProfileCode());
        response.setApplyStatus(resultModel.getApplyStatus());
        response.setRestartRequired(resultModel.getRestartRequired());
        return response;
    }

    /**
     * 将 Tracker 更新请求转换为命令。
     *
     * @param trackerSetCode 编码
     * @param request 更新请求
     * @return 更新命令
     */
    public UpdateBtTrackerSetCommand toUpdateBtTrackerSetCommand(
        String trackerSetCode,
        UpdateBtTrackerSetRequest request
    ) {
        UpdateBtTrackerSetCommand command = new UpdateBtTrackerSetCommand();
        command.setTrackerSetCode(trackerSetCode);
        command.setTrackerSetName(request.getTrackerSetName());
        command.setTrackerListText(request.getTrackerListText());
        command.setSourceUrl(request.getSourceUrl());
        return command;
    }

    /**
     * 将 Tracker 模型转换为接口响应。
     *
     * @param trackerSetModel Tracker 模型
     * @return 接口响应
     */
    public BtTrackerSetResponse toBtTrackerSetResponse(BtTrackerSetModel trackerSetModel) {
        BtTrackerSetResponse response = new BtTrackerSetResponse();
        response.setTrackerSetCode(trackerSetModel.getTrackerSetCode());
        response.setTrackerSetName(trackerSetModel.getTrackerSetName());
        response.setTrackerListText(trackerSetModel.getTrackerListText());
        response.setSourceUrl(trackerSetModel.getSourceUrl());
        return response;
    }

    /**
     * 将站点规则更新请求转换为命令。
     *
     * @param ruleId 规则 ID
     * @param request 更新请求
     * @return 更新命令
     */
    public UpdateSourceSiteRuleCommand toUpdateSourceSiteRuleCommand(
        Long ruleId,
        UpdateSourceSiteRuleRequest request
    ) {
        UpdateSourceSiteRuleCommand command = new UpdateSourceSiteRuleCommand();
        command.setRuleId(ruleId);
        command.setHostPattern(request.getHostPattern());
        command.setProfileCode(request.getProfileCode());
        command.setTrackerSetCode(request.getTrackerSetCode());
        return command;
    }

    /**
     * 将站点规则模型转换为接口响应。
     *
     * @param ruleModel 规则模型
     * @return 接口响应
     */
    public SourceSiteRuleResponse toSourceSiteRuleResponse(SourceSiteRuleModel ruleModel) {
        SourceSiteRuleResponse response = new SourceSiteRuleResponse();
        response.setId(ruleModel.getId());
        response.setHostPattern(ruleModel.getHostPattern());
        response.setProfileCode(ruleModel.getProfileCode());
        response.setTrackerSetCode(ruleModel.getTrackerSetCode());
        return response;
    }
}
