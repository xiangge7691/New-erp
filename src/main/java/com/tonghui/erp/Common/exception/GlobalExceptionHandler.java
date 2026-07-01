package com.tonghui.erp.Common.exception;

import com.tonghui.erp.Common.Dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 统一拦截所有Controller层异常，返回标准 ApiResponse 格式
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    //#region 业务异常处理
    // ===================================
    // 业务异常处理
    // ===================================

    /**
     * 处理业务运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> handleRuntimeException(RuntimeException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return ApiResponse.errorResponse(500, ex.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("参数异常: {}", ex.getMessage());
        return ApiResponse.errorResponse(ApiResponse.BAD_REQUEST, ex.getMessage());
    }

    //#endregion

    //#region 请求参数异常处理
    // ===================================
    // 请求参数异常处理
    // ===================================

    /**
     * 处理请求体参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("参数校验异常: {}", message);
        return ApiResponse.errorResponse(ApiResponse.BAD_REQUEST, message);
    }

    //#endregion

    //#region HTTP方法异常处理
    // ===================================
    // HTTP方法异常处理
    // ===================================

    /**
     * 处理不支持的HTTP方法异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("不支持的请求方法: {}", ex.getMessage());
        return ApiResponse.errorResponse(ApiResponse.METHOD_NOT_ALLOWED, "不支持的请求方法: " + ex.getMethod());
    }

    //#endregion

    //#region 兜底异常处理
    // ===================================
    // 兜底异常处理
    // ===================================

    /**
     * 处理所有未捕获的异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> handleException(Exception ex) {
        log.error("系统内部异常", ex);
        return ApiResponse.errorResponse(ApiResponse.INTERNAL_ERROR, "系统内部错误: " + ex.getMessage());
    }

    //#endregion
}
