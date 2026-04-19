package com.mooddownload.local.service.capture;

import com.mooddownload.local.common.constant.HeaderConstants;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.service.capture.convert.CaptureTaskConverter;
import com.mooddownload.local.service.capture.model.CaptureTaskResult;
import com.mooddownload.local.service.capture.model.ClipboardCaptureCommand;
import com.mooddownload.local.service.config.ConfigService;
import com.mooddownload.local.service.task.TaskWorkflowService;
import com.mooddownload.local.service.task.model.TaskOperationResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 剪贴板接入服务。
 */
@Service
public class ClipboardCaptureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClipboardCaptureService.class);

    private static final Pattern DOWNLOAD_URI_PATTERN = Pattern.compile(
        "(magnet:\\?\\S+|https?://\\S+|bt://\\S+)",
        Pattern.CASE_INSENSITIVE
    );

    private final ConfigService configService;

    private final TaskWorkflowService taskWorkflowService;

    private final CaptureTaskConverter captureTaskConverter;

    public ClipboardCaptureService(
        ConfigService configService,
        TaskWorkflowService taskWorkflowService,
        CaptureTaskConverter captureTaskConverter
    ) {
        this.configService = configService;
        this.taskWorkflowService = taskWorkflowService;
        this.captureTaskConverter = captureTaskConverter;
    }

    /**
     * 从剪贴板文本中识别下载地址并创建任务。
     *
     * @param command 剪贴板接入命令
     * @return 接入结果
     */
    public CaptureTaskResult capture(ClipboardCaptureCommand command) {
        validateCommand(command);
        configService.ensureClipboardMonitorEnabled();
        String downloadUrl = extractDownloadUrl(command.getClipboardText());
        TaskOperationResult taskOperationResult = taskWorkflowService.createTask(captureTaskConverter.toCreateTaskCommand(
            command,
            downloadUrl,
            configService.resolveSaveDir(null)
        ));
        LOGGER.info("处理剪贴板接入成功: clientRequestId={}, taskId={}, clientType={}, downloadUrl={}",
            command.getClientRequestId(),
            taskOperationResult.getTaskModel().getId(),
            command.getClientType(),
            downloadUrl);
        return captureTaskConverter.toCaptureTaskResult(taskOperationResult);
    }

    private void validateCommand(ClipboardCaptureCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "剪贴板接入命令不能为空");
        }
        if (!StringUtils.hasText(command.getClientRequestId())) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "clientRequestId 不能为空");
        }
        if (!StringUtils.hasText(command.getClipboardText())) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "clipboardText 不能为空");
        }
        if (!StringUtils.hasText(command.getClientType())) {
            command.setClientType(HeaderConstants.CLIENT_TYPE_DESKTOP_APP);
        }
    }

    private String extractDownloadUrl(String clipboardText) {
        Matcher matcher = DOWNLOAD_URI_PATTERN.matcher(clipboardText);
        if (!matcher.find()) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "剪贴板内容中未识别到受支持的下载地址");
        }
        return trimTrailingDelimiters(matcher.group(1));
    }

    private String trimTrailingDelimiters(String candidate) {
        String normalizedCandidate = candidate;
        while (normalizedCandidate.endsWith(".")
            || normalizedCandidate.endsWith(",")
            || normalizedCandidate.endsWith(";")
            || normalizedCandidate.endsWith(")")
            || normalizedCandidate.endsWith("]")) {
            normalizedCandidate = normalizedCandidate.substring(0, normalizedCandidate.length() - 1);
        }
        return normalizedCandidate;
    }
}
