package com.mooddownload.local.controller.config;

import com.mooddownload.local.common.response.ApiResponse;
import com.mooddownload.local.controller.config.convert.ConfigControllerConverter;
import com.mooddownload.local.controller.config.vo.DownloadConfigResponse;
import com.mooddownload.local.controller.config.vo.UpdateDownloadConfigRequest;
import com.mooddownload.local.service.config.ConfigService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置接口控制器。
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigService configService;

    private final ConfigControllerConverter configControllerConverter;

    public ConfigController(
        ConfigService configService,
        ConfigControllerConverter configControllerConverter
    ) {
        this.configService = configService;
        this.configControllerConverter = configControllerConverter;
    }

    /**
     * 查询当前下载配置。
     *
     * @return 统一响应
     */
    @GetMapping
    public ApiResponse<DownloadConfigResponse> getConfig() {
        return ApiResponse.success(configControllerConverter.toResponse(configService.getCurrentConfig()));
    }

    /**
     * 更新下载配置。
     *
     * @param request 更新请求
     * @return 统一响应
     */
    @PutMapping
    public ApiResponse<DownloadConfigResponse> updateConfig(@Valid @RequestBody UpdateDownloadConfigRequest request) {
        return ApiResponse.success(configControllerConverter.toResponse(
            configService.updateConfig(configControllerConverter.toUpdateCommand(request))
        ));
    }
}
