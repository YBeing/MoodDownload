package com.mooddownload.local.controller.config;

import com.mooddownload.local.common.response.ApiResponse;
import com.mooddownload.local.controller.config.convert.ConfigControllerConverter;
import com.mooddownload.local.controller.config.vo.EngineRuntimeApplyRequest;
import com.mooddownload.local.controller.config.vo.EngineRuntimeApplyResponse;
import com.mooddownload.local.controller.config.vo.EngineRuntimeProfileResponse;
import com.mooddownload.local.controller.config.vo.UpdateEngineRuntimeProfileRequest;
import com.mooddownload.local.service.config.EngineRuntimeProfileService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 引擎运行配置接口控制器。
 */
@RestController
@RequestMapping("/api/config/engine-runtime")
public class EngineRuntimeProfileController {

    private final EngineRuntimeProfileService engineRuntimeProfileService;

    private final ConfigControllerConverter configControllerConverter;

    public EngineRuntimeProfileController(
        EngineRuntimeProfileService engineRuntimeProfileService,
        ConfigControllerConverter configControllerConverter
    ) {
        this.engineRuntimeProfileService = engineRuntimeProfileService;
        this.configControllerConverter = configControllerConverter;
    }

    /**
     * 查询引擎运行配置中心快照。
     *
     * @return 统一响应
     */
    @GetMapping
    public ApiResponse<EngineRuntimeProfileResponse> getEngineRuntimeProfile() {
        return ApiResponse.success(configControllerConverter.toEngineRuntimeProfileResponse(
            engineRuntimeProfileService.getSnapshot()
        ));
    }

    /**
     * 更新引擎运行配置。
     *
     * @param request 更新请求
     * @return 统一响应
     */
    @PutMapping
    public ApiResponse<EngineRuntimeProfileResponse> updateEngineRuntimeProfile(
        @Valid @RequestBody UpdateEngineRuntimeProfileRequest request
    ) {
        return ApiResponse.success(configControllerConverter.toEngineRuntimeProfileResponse(
            engineRuntimeProfileService.updateProfile(
                configControllerConverter.toUpdateEngineRuntimeProfileCommand(request)
            )
        ));
    }

    /**
     * 应用引擎运行配置。
     *
     * @param request 应用请求
     * @return 统一响应
     */
    @PostMapping("/apply")
    public ApiResponse<EngineRuntimeApplyResponse> applyEngineRuntimeProfile(
        @Valid @RequestBody EngineRuntimeApplyRequest request
    ) {
        return ApiResponse.success(configControllerConverter.toEngineRuntimeApplyResponse(
            engineRuntimeProfileService.applyProfile(request.getProfileCode(), request.getForceRestart())
        ));
    }
}
