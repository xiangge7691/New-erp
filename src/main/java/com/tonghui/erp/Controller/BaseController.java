package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 控制器基类
 * <p>
 * 提供通用的Controller方法和工具方法，供其他控制器继承使用
 * </p>
 */
public abstract class BaseController {

    //#region 分页处理方法
    // ===================================
    // 分页处理方法
    // ===================================
    
    /**
     * 处理分页请求参数
     * 
     * @param pageRequest 分页请求参数
     * @return 处理后的分页请求参数
     */
    protected PageRequestDto processPageRequest(PageRequestDto pageRequest) {
        if (pageRequest == null) {
            pageRequest = new PageRequestDto();
        }
        pageRequest.validateAndFix();
        return pageRequest;
    }

    /**
     * 处理全量数据查询的分页结果
     * 
     * @param <T> 数据类型泛型参数
     * @param result 分页结果
     * @return 处理后的分页结果
     */
    protected <T> PagedResult<T> processAllDataResult(PagedResult<T> result) {
        result.setPageSize((int) result.getTotalCount());
        return result;
    }
    
    //#endregion

    //#region 响应构建方法
    // ===================================
    // 响应构建方法
    // ===================================
    
    /**
     * 构建成功响应
     * 
     * @param <T> 数据类型泛型参数
     * @param data 数据
     * @return 成功响应
     */
    protected <T> ApiResponse<T> success(T data) {
        return ApiResponse.successResponse(data);
    }

    /**
     * 构建带消息的成功响应
     * 
     * @param <T> 数据类型泛型参数
     * @param data 数据
     * @param message 消息
     * @return 成功响应
     */
    protected <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.successResponse(data, message);
    }

    /**
     * 构建错误响应
     * 
     * @param <T> 数据类型泛型参数
     * @param message 错误消息
     * @return 错误响应
     */
    protected <T> ApiResponse<T> error(String message) {
        return ApiResponse.errorResponse(message);
    }

    /**
     * 构建异常响应
     * 
     * @param <T> 数据类型泛型参数
     * @param ex 异常
     * @param operation 操作名称
     * @return 错误响应
     */
    protected <T> ApiResponse<T> exception(Exception ex, String operation) {
        return ApiResponse.errorResponse(operation + "失败: " + ex.getMessage());
    }
    
    //#endregion
}
