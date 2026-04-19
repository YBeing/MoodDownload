package com.mooddownload.local.service.capture;

import com.mooddownload.local.common.constant.HeaderConstants;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.service.capture.convert.CaptureTaskConverter;
import com.mooddownload.local.service.capture.model.CaptureTaskResult;
import com.mooddownload.local.service.capture.model.ExtensionCaptureCommand;
import com.mooddownload.local.service.config.ConfigService;
import com.mooddownload.local.service.task.TaskWorkflowService;
import com.mooddownload.local.service.task.model.TaskOperationResult;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 浏览器扩展接管服务。
 */
@Service
public class ExtensionCaptureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionCaptureService.class);

    private static final Set<String> SUPPORTED_BROWSERS = new HashSet<String>(Arrays.asList("chrome", "edge"));

    private final ConfigService configService;

    private final TaskWorkflowService taskWorkflowService;

    private final CaptureTaskConverter captureTaskConverter;

    public ExtensionCaptureService(
        ConfigService configService,
        TaskWorkflowService taskWorkflowService,
        CaptureTaskConverter captureTaskConverter
    ) {
        this.configService = configService;
        this.taskWorkflowService = taskWorkflowService;
        this.captureTaskConverter = captureTaskConverter;
    }

    /**
     * 承接扩展转发的下载请求，并复用任务域创建下载任务。
     *
     * @param command 扩展接管命令
     * @return 接管结果
     */
    public CaptureTaskResult capture(ExtensionCaptureCommand command) {
        validateCommand(command);
        configService.ensureBrowserCaptureEnabled();
        TaskOperationResult taskOperationResult = taskWorkflowService.createTask(
            captureTaskConverter.toCreateTaskCommand(command, configService.resolveSaveDir(null))
        );
        LOGGER.info("处理扩展接管请求成功: clientRequestId={}, browser={}, taskId={}, tabUrl={}",
            command.getClientRequestId(),
            command.getBrowser(),
            taskOperationResult.getTaskModel().getId(),
            command.getTabUrl());
        return captureTaskConverter.toCaptureTaskResult(taskOperationResult);
    }

    private void validateCommand(ExtensionCaptureCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "扩展接管命令不能为空");
        }
        if (!HeaderConstants.CLIENT_TYPE_NATIVE_HOST.equalsIgnoreCase(command.getClientType())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "仅允许 native-host 调用扩展接管接口");
        }
        if (!StringUtils.hasText(command.getClientRequestId())) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "clientRequestId 不能为空");
        }
        String browser = normalizeBrowser(command.getBrowser());
        if (!SUPPORTED_BROWSERS.contains(browser)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "不支持的浏览器类型: " + command.getBrowser());
        }
        if (!StringUtils.hasText(command.getDownloadUrl())) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "downloadUrl 不能为空");
        }
        command.setBrowser(browser);
    }

    private String normalizeBrowser(String browser) {
        return StringUtils.hasText(browser) ? browser.trim().toLowerCase(Locale.ROOT) : null;
    }
}
