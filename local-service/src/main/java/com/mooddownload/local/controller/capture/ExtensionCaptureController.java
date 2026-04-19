package com.mooddownload.local.controller.capture;

import com.mooddownload.local.common.response.ApiResponse;
import com.mooddownload.local.common.security.RequestContext;
import com.mooddownload.local.controller.capture.convert.ExtensionCaptureControllerConverter;
import com.mooddownload.local.controller.capture.vo.ExtensionCaptureRequest;
import com.mooddownload.local.controller.capture.vo.ExtensionCaptureResponse;
import com.mooddownload.local.service.capture.ExtensionCaptureService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 浏览器扩展接管入口控制器。
 */
@RestController
@RequestMapping("/api/extension")
public class ExtensionCaptureController {

    private final ExtensionCaptureService extensionCaptureService;

    private final ExtensionCaptureControllerConverter extensionCaptureControllerConverter;

    public ExtensionCaptureController(
        ExtensionCaptureService extensionCaptureService,
        ExtensionCaptureControllerConverter extensionCaptureControllerConverter
    ) {
        this.extensionCaptureService = extensionCaptureService;
        this.extensionCaptureControllerConverter = extensionCaptureControllerConverter;
    }

    /**
     * 接收浏览器扩展转发的下载接管请求。
     *
     * @param request 接管请求
     * @return 统一响应
     */
    @PostMapping("/capture")
    public ApiResponse<ExtensionCaptureResponse> capture(@Valid @RequestBody ExtensionCaptureRequest request) {
        return ApiResponse.success(extensionCaptureControllerConverter.toResponse(extensionCaptureService.capture(
            extensionCaptureControllerConverter.toCommand(request, RequestContext.getClientType())
        )));
    }
}
