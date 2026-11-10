package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据变更日志表
 * <p>
 * 记录每条数据修改前后的字段值，同一请求内的主记录与级联记录通过 trace_id 关联
 * </p>
 *
 * @TableName sys_data_change_log
 */
@TableName(value = "sys_data_change_log")
@Data
public class SysDataChangeLog {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 变更日志ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 请求变更TraceID
     */
    @TableField(value = "trace_id")
    private String traceId;

    /**
     * 关联操作日志ID
     */
    @TableField(value = "operation_log_id")
    private Long operationLogId;

    // endregion

    // region 变更对象字段
    // ===================================
    // 变更对象字段
    // ===================================

    /**
     * 表名
     */
    @TableField(value = "table_name")
    private String tableName;

    /**
     * 实体类全名
     */
    @TableField(value = "entity_class")
    private String entityClass;

    /**
     * 业务记录主键值
     */
    @TableField(value = "business_id")
    private String businessId;

    /**
     * 变更类型：INSERT/UPDATE/DELETE
     */
    @TableField(value = "change_type")
    private String changeType;

    /**
     * 来源：PRIMARY主记录/RELATED关联变更
     */
    @TableField(value = "change_source")
    private String changeSource;

    // endregion

    // region 变更内容字段
    // ===================================
    // 变更内容字段
    // ===================================

    /**
     * 修改前整行快照（脱敏、截断）
     */
    @TableField(value = "before_json")
    private String beforeJson;

    /**
     * 修改后整行快照（脱敏、截断）
     */
    @TableField(value = "after_json")
    private String afterJson;

    /**
     * 字段差异JSON：[{field,label,before,after}]
     */
    @TableField(value = "changed_fields")
    private String changedFields;

    /**
     * 发生变化的字段数
     */
    @TableField(value = "field_count")
    private Integer fieldCount;

    // endregion

    // region 操作人与时间字段
    // ===================================
    // 操作人与时间字段
    // ===================================

    /**
     * 操作人ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 操作人账号
     */
    @TableField(value = "user_account")
    private String userAccount;

    /**
     * 操作人姓名
     */
    @TableField(value = "user_name")
    private String userName;

    /**
     * 变更时间
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

    // endregion
}
