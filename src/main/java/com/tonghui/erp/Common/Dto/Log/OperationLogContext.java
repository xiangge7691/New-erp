package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志上下文
 * <p>
 * 由切面在请求线程中组装完成，再交给异步服务落库。
 * 用户信息必须在进入异步线程前采集完毕。
 * </p>
 */
@Data
public class OperationLogContext {

    // region 用户字段
    // ===================================
    // 用户字段
    // ===================================

    /**
     * 操作人ID
     */
    private Long userId;

    /**
     * 操作人账号（异步落库时按用户ID补全）
     */
    private String userAccount;

    /**
     * 操作人姓名（异步落库时按用户ID补全）
     */
    private String userName;

    // endregion

    // region 操作信息字段
    // ===================================
    // 操作信息字段
    // ===================================

    /**
     * 业务模块
     */
    private String module;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作描述
     */
    private String operationDesc;

    /**
     * 业务对象类型
     */
    private String businessType;

    /**
     * 业务对象ID
     */
    private String businessId;

    // endregion

    // region 请求与结果字段
    // ===================================
    // 请求与结果字段
    // ===================================

    /**
     * HTTP方法
     */
    private String requestMethod;

    /**
     * 请求地址
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

    /**
     * 业务响应码
     */
    private Integer responseCode;

    /**
     * 结果：1成功/0失败
     */
    private Integer resultStatus = 1;

    /**
     * 失败时的异常信息
     */
    private String errorMsg;

    /**
     * 耗时（毫秒）
     */
    private Integer costTime;

    // endregion

    // region 客户端字段
    // ===================================
    // 客户端字段
    // ===================================

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 客户端MAC地址（请求头 X-Client-Mac 上报）
     */
    private String macAddress;

    /**
     * IP归属地
     */
    private String ipLocation;

    /**
     * 客户端User-Agent
     */
    private String userAgent;

    /**
     * 操作时间（请求进入时间）
     */
    private LocalDateTime createdTime;

    // endregion

    // region 数据变更字段
    // ===================================
    // 数据变更字段
    // ===================================

    /**
     * 变更TraceID
     */
    private String traceId;

    /**
     * 本次操作产生的数据变更
     */
    private List<DataChangeRecord> changes = new ArrayList<>();

    /**
     * 变更是否因为超过上限被截断
     */
    private boolean changeTruncated;

    // endregion
}
