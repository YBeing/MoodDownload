package com.mooddownload.local.controller.provider;

import com.mooddownload.local.common.response.ApiResponse;
import com.mooddownload.local.controller.provider.convert.BaiduPanProviderControllerConverter;
import com.mooddownload.local.controller.provider.vo.BaiduPanPreflightRequest;
import com.mooddownload.local.controller.provider.vo.BaiduPanPreflightResponse;
import com.mooddownload.local.controller.provider.vo.BaiduPanResolveRequest;
import com.mooddownload.local.controller.provider.vo.BaiduPanResolveResponse;
import com.mooddownload.local.service.provider.BaiduPanProviderService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 百度网盘预研接口控制器。
 */
@RestController
@RequestMapping("/api/providers/baidupan")
public class BaiduPanProviderController {

    private final BaiduPanProviderService baiduPanProviderService;

    private final BaiduPanProviderControllerConverter baiduPanProviderControllerConverter;

    public BaiduPanProviderController(
        BaiduPanProviderService baiduPanProviderService,
        BaiduPanProviderControllerConverter baiduPanProviderControllerConverter
    ) {
        this.baiduPanProviderService = baiduPanProviderService;
        this.baiduPanProviderControllerConverter = baiduPanProviderControllerConverter;
    }

    /**
     * 执行百度网盘预研校验。
     *
     * @param request 预检请求
     * @return 统一响应
     */
    @PostMapping("/preflight")
    public ApiResponse<BaiduPanPreflightResponse> preflight(@Valid @RequestBody BaiduPanPreflightRequest request) {
        return ApiResponse.success(baiduPanProviderControllerConverter.toPreflightResponse(
            baiduPanProviderService.preflight(request.getShareUrl(), request.getAuthContext())
        ));
    }

    /**
     * 执行百度网盘预研解析。
     *
     * @param request 解析请求
     * @return 统一响应
     */
    @PostMapping("/resolve")
    public ApiResponse<BaiduPanResolveResponse> resolve(@Valid @RequestBody BaiduPanResolveRequest request) {
        return ApiResponse.success(baiduPanProviderControllerConverter.toResolveResponse(
            baiduPanProviderService.resolve(request.getProviderContext())
        ));
    }
}
