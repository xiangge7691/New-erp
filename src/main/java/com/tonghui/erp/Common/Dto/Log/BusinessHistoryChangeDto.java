package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;

/**
 * 业务变更历史中的单个字段变更
 * <p>
 * 一个字段对应一条前端可渲染的改前/改后记录
 * </p>
 */
@Data
public class BusinessHistoryChangeDto {

    /**
     * 数据变更日志ID
     */
    private Long dataChangeId;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 变更记录主键值
     */
    private String businessId;

    /**
     * 关联记录名称（如处方行对应的物料名称）
     */
    private String recordLabel;

    /**
     * 变更类型：INSERT/UPDATE/DELETE
     */
    private String changeType;

    /**
     * 变更来源：PRIMARY主记录/RELATED关联变更
     */
    private String changeSource;

    /**
     * 字段名（数据库列名）
     */
    private String field;

    /**
     * 字段中文名
     */
    private String label;

    /**
     * 修改前值
     */
    private Object before;

    /**
     * 修改后值
     */
    private Object after;
}
