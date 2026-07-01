package com.tonghui.erp.Common.Dto;

import lombok.Data;

/**
 * API响应结果
 * <p>
 * 用于封装API接口的统一响应格式，包含状态码、消息和数据
 * </p>
 * 
 * @param <T> 数据类型
 */
@Data
public class ApiResponse<T> {

    //#region 错误码常量
    // ===================================
    // 错误码常量
    // ===================================

    /** 请求成功 */
    public static final int SUCCESS = 200;
    /** 请求参数错误 */
    public static final int BAD_REQUEST = 400;
    /** 未授权/未登录 */
    public static final int UNAUTHORIZED = 401;
    /** 禁止访问 */
    public static final int FORBIDDEN = 403;
    /** 资源不存在 */
    public static final int NOT_FOUND = 404;
    /** 不支持的请求方法 */
    public static final int METHOD_NOT_ALLOWED = 405;
    /** 服务器内部错误 */
    public static final int INTERNAL_ERROR = 500;
    //#endregion

    //#region 响应字段
    // ===================================
    // 响应字段
    // ===================================

    /**
     * 业务状态码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private long timestamp = System.currentTimeMillis();
    //#endregion

    //#region 成功响应构建方法
    // ===================================
    // 成功响应构建方法
    // ===================================

    /**
     * 创建成功的响应
     * @param data 数据
     * @param message 消息
     * @param <T> 数据类型
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> successResponse(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(SUCCESS);
        response.setData(data);
        response.setMessage(message != null ? message : "操作成功");
        return response;
    }

    /**
     * 创建成功的响应（默认消息）
     * @param data 数据
     * @param <T> 数据类型
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> successResponse(T data) {
        return successResponse(data, null);
    }
    //#endregion

    //#region 错误响应构建方法
    // ===================================
    // 错误响应构建方法
    // ===================================

    /**
     * 创建失败的响应
     * @param code 错误码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> errorResponse(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    /**
     * 创建失败的响应（默认错误码500）
     * @param message 错误消息
     * @param <T> 数据类型
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> errorResponse(String message) {
        return errorResponse(INTERNAL_ERROR, message);
    }
    //#endregion

    //#region 便捷错误响应方法
    // ===================================
    // 便捷错误响应方法
    // ===================================

    /**
     * 创建参数错误响应（400）
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return errorResponse(BAD_REQUEST, message);
    }

    /**
     * 创建未授权响应（401）
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return errorResponse(UNAUTHORIZED, message);
    }

    /**
     * 创建禁止访问响应（403）
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return errorResponse(FORBIDDEN, message);
    }

    /**
     * 创建资源不存在响应（404）
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return errorResponse(NOT_FOUND, message);
    }
    //#endregion
}
