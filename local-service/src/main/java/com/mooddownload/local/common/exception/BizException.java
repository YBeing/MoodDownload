package com.mooddownload.local.common.exception;

import com.mooddownload.local.common.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 业务异常基类。
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    private final HttpStatus httpStatus;

    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), errorCode.getHttpStatus());
    }

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode, message, errorCode.getHttpStatus());
    }

    public BizException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}

