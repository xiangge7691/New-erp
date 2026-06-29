package com.tonghui.erp.Common.Dto;

import lombok.Data;

/**
 * API响应结果 - Rust风格
 * <p>
 * 用于封装API接口的统一响应格式，包含状态码、消息和数据
 * </p>
 * 
 * @param <T> 数据类型
 */
@Data
public class ApiResponse<T> {
    
    //#region 响应字段
    // ===================================
    // 响应字段
    // ===================================
    
    /**
     * HTTP状态码
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
        response.setCode(200);
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
        return errorResponse(500, message);
    }
    //#endregion
}
