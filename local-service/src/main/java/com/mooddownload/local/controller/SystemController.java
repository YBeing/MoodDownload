package com.mooddownload.local.controller;

import com.mooddownload.local.common.response.ApiResponse;
import java.util.Collections;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统基础探针接口。
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    /**
     * 返回基础运行状态，供本地调试与 smoke test 使用。
     *
     * @return 统一响应
     */
    @GetMapping("/ping")
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.success(Collections.singletonMap("status", "pong"));
    }
}

