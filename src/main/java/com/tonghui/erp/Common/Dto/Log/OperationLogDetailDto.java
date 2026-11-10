package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志详情
 * <p>
 * 在列表字段基础上补充请求地址、参数摘要、异常信息与客户端信息，
 * 用于前端“查看详情”抽屉展示
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogDetailDto extends OperationLogDto {

    // region 请求信息字段
    // ===================================
    // 请求信息字段
    // ===================================

    /**
     * HTTP方法
     */
    private String requestMethod;

    /**
     * 请求地址（含查询串）
     */
    private String requestUrl;

    /**
     * 执行的方法全名
     */
    private String classMethod;

    /**
     * 请求参数摘要（已脱敏）
     */
    private String requestParams;

    // endregion

    // region 异常与客户端字段
    // ===================================
    // 异常与客户端字段
    // ===================================

    /**
     * 失败时的异常信息
     */
    private String errorMsg;

    /**
     * 客户端User-Agent
     */
    private String userAgent;

    /**
     * 变更TraceID
     */
    private String traceId;

    // endregion
}
