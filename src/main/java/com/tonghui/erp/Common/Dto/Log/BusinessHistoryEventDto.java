package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务变更历史事件
 * <p>
 * 一次操作（一个操作日志或一个 TraceID）产生的全部字段变更，按操作归组
 * </p>
 */
@Data
public class BusinessHistoryEventDto {

    // region 操作信息字段
    // ===================================
    // 操作信息字段
    // ===================================

    /**
     * 关联操作日志ID（可能为空）
     */
    private Long operationLogId;

    /**
     * 变更TraceID
     */
    private String traceId;

    /**
     * 操作日志记录的业务类型
     */
    private String operationBusinessType;

    /**
     * 业务模块
     */
    private String module;

    /**
     * 操作类型：CREATE/UPDATE/DELETE/APPROVE 等
     */
    private String operationType;

    /**
     * 操作描述
     */
    private String operationDesc;

    // endregion

    // region 操作人与终端字段
    // ===================================
    // 操作人与终端字段
    // ===================================

    /**
     * 操作人姓名
     */
    private String userName;

    /**
     * 操作人账号
     */
    private String userAccount;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * IP归属地
     */
    private String ipLocation;

    /**
     * 客户端MAC地址
     */
    private String macAddress;

    /**
     * 操作时间
     */
    private LocalDateTime createdTime;

    // endregion

    // region 字段变更字段
    // ===================================
    // 字段变更字段
    // ===================================

    /**
     * 本次操作产生的字段变更
     */
    private List<BusinessHistoryChangeDto> changes = new ArrayList<>();

    // endregion
}
