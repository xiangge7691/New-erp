package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志列表项
 * <p>
 * 列表接口不返回请求参数、异常信息等大字段，详情请查询 OperationLogDetailDto
 * </p>
 */
@Data
public class OperationLogDto {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 操作人ID
     */
    private Long userId;

    /**
     * 操作人账号
     */
    private String userAccount;

    /**
     * 操作人姓名
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

    // region 结果与客户端字段
    // ===================================
    // 结果与客户端字段
    // ===================================

    /**
     * 业务响应码
     */
    private Integer responseCode;

    /**
     * 结果：1成功/0失败
     */
    private Integer resultStatus;

    /**
     * 耗时（毫秒）
     */
    private Integer costTime;

    /**
     * 数据变更记录数
     */
    private Integer changeCount;

    /**
     * 数据变更是否截断：0否/1是
     */
    private Integer changeTruncated;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 客户端MAC地址（客户端通过 X-Client-Mac 上报，格式 AA-BB-CC-DD-EE-FF）
     */
    private String macAddress;

    /**
     * IP归属地
     */
    private String ipLocation;

    /**
     * 操作时间
     */
    private LocalDateTime createdTime;

    // endregion
}
