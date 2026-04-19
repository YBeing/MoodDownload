package com.mooddownload.local.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.security.RequestContext;

/**
 * 统一响应体。
 *
 * @param <T> 响应数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final String code;

    private final String message;

    private final String requestId;

    private final T data;

    private ApiResponse(String code, String message, String requestId, T data) {
        this.code = code;
        this.message = message;
        this.requestId = requestId;
        this.data = data;
    }

    /**
     * 构建成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 统一响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(
            ErrorCode.SUCCESS.getCode(),
            ErrorCode.SUCCESS.getDefaultMessage(),
            RequestContext.getRequestId(),
            data
        );
    }

    /**
     * 构建失败响应。
     *
     * @param errorCode 错误码
     * @param message 错误描述
     * @return 统一响应对象
     */
    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<Void>(
            errorCode.getCode(),
            message,
            RequestContext.getRequestId(),
            null
        );
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getRequestId() {
        return requestId;
    }

    public T getData() {
        return data;
    }
}

