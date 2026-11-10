package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据变更日志列表项
 * <p>
 * 列表接口返回字段差异，不返回整行前后 JSON
 * </p>
 */
@Data
public class DataChangeLogDto {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 变更日志ID
     */
    private Long id;

    /**
     * 请求变更TraceID
     */
    private String traceId;

    /**
     * 关联操作日志ID
     */
    private Long operationLogId;

    // endregion

    // region 变更对象字段
    // ===================================
    // 变更对象字段
    // ===================================

    /**
     * 表名
     */
    private String tableName;

    /**
     * 实体类全名
     */
    private String entityClass;

    /**
     * 业务记录主键值
     */
    private String businessId;

    /**
     * 变更类型：INSERT/UPDATE/DELETE
     */
    private String changeType;

    /**
     * 来源：PRIMARY主记录/RELATED关联变更
     */
    private String changeSource;

    /**
     * 发生变化的字段数
     */
    private Integer fieldCount;

    /**
     * 字段差异
     */
    private List<FieldChange> changedFields = new ArrayList<>();

    // endregion

    // region 操作人与时间字段
    // ===================================
    // 操作人与时间字段
    // ===================================

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

    /**
     * 变更时间
     */
    private LocalDateTime createdTime;

    // endregion
}
