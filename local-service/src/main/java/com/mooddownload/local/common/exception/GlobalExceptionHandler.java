package com.mooddownload.local.common.exception;

import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.response.ApiResponse;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常并返回统一响应。
     *
     * @param exception 业务异常
     * @return 统一响应
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException exception) {
        if (exception.getHttpStatus().is5xxServerError()) {
            LOGGER.error("业务异常触发服务端失败: code={}, message={}",
                exception.getErrorCode().getCode(), exception.getMessage(), exception);
        } else {
            LOGGER.warn("业务异常: code={}, message={}",
                exception.getErrorCode().getCode(), exception.getMessage());
        }
        return buildResponse(exception.getErrorCode(), exception.getMessage(), exception.getHttpStatus());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        ConstraintViolationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        String message = resolveValidationMessage(exception);
        LOGGER.warn("请求参数校验失败: {}", message);
        return buildResponse(ErrorCode.COMMON_PARAM_INVALID, message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        LOGGER.error("系统未处理异常", exception);
        return buildResponse(
            ErrorCode.INTERNAL_ERROR,
            ErrorCode.INTERNAL_ERROR.getDefaultMessage(),
            ErrorCode.INTERNAL_ERROR.getHttpStatus()
        );
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(
        ErrorCode errorCode,
        String message,
        HttpStatus httpStatus
    ) {
        return ResponseEntity.status(httpStatus).body(ApiResponse.failure(errorCode, message));
    }

    private String resolveValidationMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException validException = (MethodArgumentNotValidException) exception;
            return validException.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        }
        if (exception instanceof BindException) {
            BindException bindException = (BindException) exception;
            return bindException.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        }
        if (exception instanceof MissingServletRequestParameterException) {
            MissingServletRequestParameterException parameterException =
                (MissingServletRequestParameterException) exception;
            return "缺少必要参数: " + parameterException.getParameterName();
        }
        if (exception instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException mismatchException = (MethodArgumentTypeMismatchException) exception;
            return "参数类型不匹配: " + mismatchException.getName();
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return "请求体格式错误";
        }
        if (exception instanceof ConstraintViolationException) {
            ConstraintViolationException violationException = (ConstraintViolationException) exception;
            return violationException.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining("; "));
        }
        return ErrorCode.COMMON_PARAM_INVALID.getDefaultMessage();
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + " " + fieldError.getDefaultMessage();
    }
}

