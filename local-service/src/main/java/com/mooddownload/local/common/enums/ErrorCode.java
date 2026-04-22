package com.mooddownload.local.common.enums;

import org.springframework.http.HttpStatus;

/**
 * 系统通用错误码定义。
 */
public enum ErrorCode {

    SUCCESS("0", "ok", HttpStatus.OK),
    COMMON_PARAM_INVALID("COMMON_PARAM_INVALID", "参数非法", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", "请求未授权", HttpStatus.UNAUTHORIZED),
    TASK_NOT_FOUND("TASK_NOT_FOUND", "任务不存在", HttpStatus.NOT_FOUND),
    TASK_STATE_INVALID("TASK_STATE_INVALID", "任务状态不允许当前操作", HttpStatus.CONFLICT),
    TASK_RETRY_NOT_ALLOWED("TASK_RETRY_NOT_ALLOWED", "当前任务不允许重试", HttpStatus.CONFLICT),
    TASK_MAX_RETRY_EXCEEDED("TASK_MAX_RETRY_EXCEEDED", "已超过最大重试次数", HttpStatus.CONFLICT),
    TASK_CREATE_FAILED("TASK_CREATE_FAILED", "创建任务失败", HttpStatus.INTERNAL_SERVER_ERROR),
    TASK_DELETE_FAILED("TASK_DELETE_FAILED", "删除任务失败", HttpStatus.INTERNAL_SERVER_ERROR),
    TORRENT_PARSE_FAILED("TORRENT_PARSE_FAILED", "种子解析失败", HttpStatus.BAD_REQUEST),
    CAPTURE_DISABLED("CAPTURE_DISABLED", "接入能力未开启", HttpStatus.CONFLICT),
    ENGINE_PROFILE_INVALID("ENGINE_PROFILE_INVALID", "引擎配置非法", HttpStatus.BAD_REQUEST),
    ENGINE_PROFILE_APPLY_FAILED("ENGINE_PROFILE_APPLY_FAILED", "引擎配置应用失败", HttpStatus.INTERNAL_SERVER_ERROR),
    TRACKER_SET_INVALID("TRACKER_SET_INVALID", "Tracker 配置非法", HttpStatus.BAD_REQUEST),
    SITE_RULE_INVALID("SITE_RULE_INVALID", "站点规则非法", HttpStatus.BAD_REQUEST),
    SSE_SUBSCRIBE_FAILED("SSE_SUBSCRIBE_FAILED", "事件订阅失败", HttpStatus.INTERNAL_SERVER_ERROR),
    PROVIDER_PRE_RESEARCH_ONLY("PROVIDER_PRE_RESEARCH_ONLY", "当前仅提供预研能力", HttpStatus.NOT_IMPLEMENTED),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "资源不存在", HttpStatus.NOT_FOUND),
    STATE_CONFLICT("STATE_CONFLICT", "状态冲突", HttpStatus.CONFLICT),
    EXTERNAL_ENGINE_ERROR("EXTERNAL_ENGINE_ERROR", "外部引擎调用失败", HttpStatus.BAD_GATEWAY),
    INTERNAL_ERROR("INTERNAL_ERROR", "系统内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;

    private final String defaultMessage;

    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
