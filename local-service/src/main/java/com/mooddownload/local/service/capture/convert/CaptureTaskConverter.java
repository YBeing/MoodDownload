package com.mooddownload.local.service.capture.convert;

import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.service.capture.model.CaptureTaskResult;
import com.mooddownload.local.service.capture.model.ClipboardCaptureCommand;
import com.mooddownload.local.service.capture.model.ExtensionCaptureCommand;
import com.mooddownload.local.service.task.model.CreateTaskCommand;
import com.mooddownload.local.service.task.model.TaskOperationResult;
import com.mooddownload.local.service.task.state.TaskSourceType;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * capture 场景与任务场景之间的模型转换器。
 */
@Component
public class CaptureTaskConverter {

    /**
     * 将扩展接管命令转换为任务创建命令。
     *
     * @param command 扩展接管命令
     * @param resolvedSaveDir 实际保存目录
     * @return 任务创建命令
     */
    public CreateTaskCommand toCreateTaskCommand(ExtensionCaptureCommand command, String resolvedSaveDir) {
        CreateTaskCommand createTaskCommand = new CreateTaskCommand();
        createTaskCommand.setClientRequestId(command.getClientRequestId());
        createTaskCommand.setSourceType(resolveSourceTypeCode(command.getDownloadUrl()));
        createTaskCommand.setSourceUri(command.getDownloadUrl().trim());
        createTaskCommand.setDisplayName(normalizeBlank(command.getSuggestedName()));
        createTaskCommand.setSaveDir(resolvedSaveDir);
        createTaskCommand.setClientType(command.getClientType());
        return createTaskCommand;
    }

    /**
     * 将剪贴板接入命令转换为任务创建命令。
     *
     * @param command 剪贴板接入命令
     * @param downloadUrl 识别出的下载地址
     * @param resolvedSaveDir 实际保存目录
     * @return 任务创建命令
     */
    public CreateTaskCommand toCreateTaskCommand(
        ClipboardCaptureCommand command,
        String downloadUrl,
        String resolvedSaveDir
    ) {
        CreateTaskCommand createTaskCommand = new CreateTaskCommand();
        createTaskCommand.setClientRequestId(command.getClientRequestId());
        createTaskCommand.setSourceType(resolveSourceTypeCode(downloadUrl));
        createTaskCommand.setSourceUri(downloadUrl);
        createTaskCommand.setDisplayName(normalizeBlank(command.getSuggestedName()));
        createTaskCommand.setSaveDir(resolvedSaveDir);
        createTaskCommand.setClientType(command.getClientType());
        return createTaskCommand;
    }

    /**
     * 将任务场景结果转换为 capture 场景结果。
     *
     * @param taskOperationResult 任务场景结果
     * @return capture 场景结果
     */
    public CaptureTaskResult toCaptureTaskResult(TaskOperationResult taskOperationResult) {
        CaptureTaskResult captureTaskResult = new CaptureTaskResult();
        captureTaskResult.setAccepted(Boolean.TRUE);
        captureTaskResult.setTaskId(taskOperationResult.getTaskModel().getId());
        captureTaskResult.setTaskCode(taskOperationResult.getTaskModel().getTaskCode());
        captureTaskResult.setDomainStatus(taskOperationResult.getTaskModel().getDomainStatus());
        return captureTaskResult;
    }

    /**
     * 解析下载地址对应的来源类型编码。
     *
     * @param downloadUrl 下载地址
     * @return 来源类型编码
     */
    public String resolveSourceTypeCode(String downloadUrl) {
        if (!StringUtils.hasText(downloadUrl)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "downloadUrl 不能为空");
        }
        String normalizedUrl = downloadUrl.trim().toLowerCase(Locale.ROOT);
        if (normalizedUrl.startsWith("https://")) {
            return TaskSourceType.HTTPS.name();
        }
        if (normalizedUrl.startsWith("http://")) {
            return TaskSourceType.HTTP.name();
        }
        if (normalizedUrl.startsWith("magnet:?")) {
            return TaskSourceType.MAGNET.name();
        }
        if (normalizedUrl.startsWith("bt://")) {
            return TaskSourceType.BT.name();
        }
        throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "不支持的下载地址: " + downloadUrl);
    }

    private String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
