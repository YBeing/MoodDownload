package com.mooddownload.local.service.config;

import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.dal.config.DownloadConfigRepository;
import com.mooddownload.local.mapper.config.DownloadConfigDO;
import com.mooddownload.local.service.config.convert.DownloadConfigConverter;
import com.mooddownload.local.service.config.model.DownloadConfigModel;
import com.mooddownload.local.service.config.model.UpdateDownloadConfigCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 下载配置服务。
 */
@Service
public class ConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);

    private final DownloadConfigRepository downloadConfigRepository;

    private final DownloadConfigConverter downloadConfigConverter;

    public ConfigService(
        DownloadConfigRepository downloadConfigRepository,
        DownloadConfigConverter downloadConfigConverter
    ) {
        this.downloadConfigRepository = downloadConfigRepository;
        this.downloadConfigConverter = downloadConfigConverter;
    }

    /**
     * 查询当前下载配置。
     *
     * @return 下载配置
     */
    public DownloadConfigModel getCurrentConfig() {
        return downloadConfigConverter.toModel(loadConfigOrThrow());
    }

    /**
     * 更新下载配置。
     *
     * @param command 更新命令
     * @return 更新后的下载配置
     */
    @Transactional(rollbackFor = Exception.class)
    public DownloadConfigModel updateConfig(UpdateDownloadConfigCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "配置更新命令不能为空");
        }
        validateCommand(command);
        DownloadConfigDO existingConfig = loadConfigOrThrow();
        DownloadConfigDO updatedConfig = downloadConfigConverter.copy(existingConfig);
        if (StringUtils.hasText(command.getDefaultSaveDir())) {
            updatedConfig.setDefaultSaveDir(command.getDefaultSaveDir().trim());
        }
        if (command.getMaxConcurrentDownloads() != null) {
            updatedConfig.setMaxConcurrentDownloads(command.getMaxConcurrentDownloads());
        }
        if (command.getMaxGlobalDownloadSpeed() != null) {
            updatedConfig.setMaxGlobalDownloadSpeed(command.getMaxGlobalDownloadSpeed());
        }
        if (command.getMaxGlobalUploadSpeed() != null) {
            updatedConfig.setMaxGlobalUploadSpeed(command.getMaxGlobalUploadSpeed());
        }
        if (command.getBrowserCaptureEnabled() != null) {
            updatedConfig.setBrowserCaptureEnabled(downloadConfigConverter.toInt(command.getBrowserCaptureEnabled()));
        }
        if (command.getClipboardMonitorEnabled() != null) {
            updatedConfig.setClipboardMonitorEnabled(downloadConfigConverter.toInt(command.getClipboardMonitorEnabled()));
        }
        if (StringUtils.hasText(command.getActiveEngineProfileCode())) {
            updatedConfig.setActiveEngineProfileCode(command.getActiveEngineProfileCode().trim());
        }
        if (command.getDeleteToRecycleBinEnabled() != null) {
            updatedConfig.setDeleteToRecycleBinEnabled(downloadConfigConverter.toInt(command.getDeleteToRecycleBinEnabled()));
        }
        updatedConfig.setUpdatedAt(System.currentTimeMillis());
        downloadConfigRepository.saveOrUpdate(updatedConfig);
        LOGGER.info("更新下载配置成功: defaultSaveDir={}, maxConcurrentDownloads={}",
            updatedConfig.getDefaultSaveDir(), updatedConfig.getMaxConcurrentDownloads());
        return downloadConfigConverter.toModel(updatedConfig);
    }

    /**
     * 解析任务保存目录，若未显式传入则回退到默认配置。
     *
     * @param requestedSaveDir 请求中的目录
     * @return 实际保存目录
     */
    public String resolveSaveDir(String requestedSaveDir) {
        if (StringUtils.hasText(requestedSaveDir)) {
            return requestedSaveDir.trim();
        }
        return loadConfigOrThrow().getDefaultSaveDir();
    }

    /**
     * 校验浏览器接管能力是否开启。
     */
    public void ensureBrowserCaptureEnabled() {
        ensureCaptureCapabilityEnabled(loadConfigOrThrow().getBrowserCaptureEnabled(), "browserCapture", "浏览器接管未开启");
    }

    /**
     * 校验剪贴板监听能力是否开启。
     */
    public void ensureClipboardMonitorEnabled() {
        ensureCaptureCapabilityEnabled(
            loadConfigOrThrow().getClipboardMonitorEnabled(),
            "clipboardMonitor",
            "剪贴板监听未开启"
        );
    }

    private DownloadConfigDO loadConfigOrThrow() {
        return downloadConfigRepository.findSingleton()
            .orElseThrow(() -> new BizException(ErrorCode.INTERNAL_ERROR, "默认下载配置不存在"));
    }

    private void ensureCaptureCapabilityEnabled(Integer enabledFlag, String capabilityCode, String errorMessage) {
        if (enabledFlag != null && enabledFlag == 1) {
            return;
        }
        LOGGER.warn("接入能力未开启: capability={}", capabilityCode);
        throw new BizException(ErrorCode.CAPTURE_DISABLED, errorMessage);
    }

    private void validateCommand(UpdateDownloadConfigCommand command) {
        if (command.getMaxConcurrentDownloads() != null && command.getMaxConcurrentDownloads() <= 0) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "maxConcurrentDownloads 必须大于 0");
        }
        if (command.getMaxGlobalDownloadSpeed() != null && command.getMaxGlobalDownloadSpeed() < 0) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "maxGlobalDownloadSpeed 不能小于 0");
        }
        if (command.getMaxGlobalUploadSpeed() != null && command.getMaxGlobalUploadSpeed() < 0) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "maxGlobalUploadSpeed 不能小于 0");
        }
        if (StringUtils.hasText(command.getActiveEngineProfileCode())
            && command.getActiveEngineProfileCode().trim().isEmpty()) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "activeEngineProfileCode 不能为空字符串");
        }
    }
}
