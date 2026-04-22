package com.mooddownload.local.controller.capture.convert;

import com.mooddownload.local.controller.capture.vo.ExtensionCaptureRequest;
import com.mooddownload.local.controller.capture.vo.ExtensionCaptureResponse;
import com.mooddownload.local.service.capture.model.CaptureTaskResult;
import com.mooddownload.local.service.capture.model.ExtensionCaptureCommand;
import org.springframework.stereotype.Component;

/**
 * 扩展接管接口模型转换器。
 */
@Component
public class ExtensionCaptureControllerConverter {

    /**
     * 将接管请求转换为场景命令。
     *
     * @param request 接管请求
     * @param clientType 调用方类型
     * @return 场景命令
     */
    public ExtensionCaptureCommand toCommand(ExtensionCaptureRequest request, String clientType) {
        ExtensionCaptureCommand extensionCaptureCommand = new ExtensionCaptureCommand();
        extensionCaptureCommand.setClientRequestId(request.getClientRequestId());
        extensionCaptureCommand.setBrowser(request.getBrowser());
        extensionCaptureCommand.setTabUrl(request.getTabUrl());
        extensionCaptureCommand.setDownloadUrl(request.getDownloadUrl());
        extensionCaptureCommand.setSuggestedName(request.getSuggestedName());
        extensionCaptureCommand.setReferer(request.getReferer());
        extensionCaptureCommand.setUserAgent(request.getUserAgent());
        extensionCaptureCommand.setHeaderSnapshotJson(request.getHeaderSnapshotJson());
        extensionCaptureCommand.setClientType(clientType);
        return extensionCaptureCommand;
    }

    /**
     * 将场景结果转换为接口响应。
     *
     * @param captureTaskResult 场景结果
     * @return 接口响应
     */
    public ExtensionCaptureResponse toResponse(CaptureTaskResult captureTaskResult) {
        ExtensionCaptureResponse extensionCaptureResponse = new ExtensionCaptureResponse();
        extensionCaptureResponse.setAccepted(captureTaskResult.getAccepted());
        extensionCaptureResponse.setTaskId(captureTaskResult.getTaskId());
        extensionCaptureResponse.setTaskCode(captureTaskResult.getTaskCode());
        extensionCaptureResponse.setDomainStatus(captureTaskResult.getDomainStatus());
        extensionCaptureResponse.setResolvedSourceType(captureTaskResult.getResolvedSourceType());
        extensionCaptureResponse.setSiteRuleMatched(captureTaskResult.getSiteRuleMatched());
        return extensionCaptureResponse;
    }
}
