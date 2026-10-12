package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 偏差记录实体类
 * <p>
 * 记录生产过程中发生的偏差事件，包括偏差标题、关联生产任务快照、
 * 实际发生时间、发生工序、偏差描述、处理措施、处理结果及处理人等，
 * 用于GMP偏差台账的日常登记与追溯
 * </p>
 *
 * @TableName deviation_record
 */
@TableName(value = "deviation_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviationRecord extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 偏差记录唯一标识（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 偏差编号（唯一，格式PC-YYYYMMDD-NNN，系统自动生成，不可修改）
     */
    @TableField(value = "deviation_code")
    private String deviationCode;

    /**
     * 偏差标题（必填，最长200字符）
     */
    @TableField(value = "deviation_title")
    private String deviationTitle;

    // endregion

    // region 关联生产任务字段
    // ===================================
    // 关联生产任务字段
    // ===================================

    /**
     * 关联生产任务ID（可空，仅作追溯引用，不联动任务状态）
     */
    @TableField(value = "work_order_id")
    private Long workOrderId;

    /**
     * 生产任务编号（冗余快照，保存时从生产任务表带出，便于列表展示）
     */
    @TableField(value = "work_order_code")
    private String workOrderCode;

    /**
     * 制剂名称（冗余快照）
     */
    @TableField(value = "preparation_name")
    private String preparationName;

    /**
     * 批号（冗余快照）
     */
    @TableField(value = "batch_number")
    private String batchNumber;

    // endregion

    // region 偏差详情字段
    // ===================================
    // 偏差详情字段
    // ===================================

    /**
     * 实际发生时间（必填）
     */
    @TableField(value = "occur_time")
    private LocalDateTime occurTime;

    /**
     * 发生工序（可选，最长100字符）
     */
    @TableField(value = "process_name")
    private String processName;

    /**
     * 偏差描述（必填，最长5000字符）
     */
    @TableField(value = "description")
    private String description;

    /**
     * 处理措施（可选，最长5000字符）
     */
    @TableField(value = "action_desc")
    private String actionDesc;

    /**
     * 处理结果（可选，最长5000字符）
     */
    @TableField(value = "result_desc")
    private String resultDesc;

    /**
     * 处理人姓名（可选，存姓名文本，最长100字符）
     */
    @TableField(value = "handler_name")
    private String handlerName;

    /**
     * 处理时间（可选）
     */
    @TableField(value = "handle_time")
    private LocalDateTime handleTime;

    /**
     * 备注说明（可选，最长1000字符）
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 审计与软删除字段
    // ===================================
    // 审计与软删除字段
    // ===================================

    /**
     * 是否已删除：0否/1是
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    // endregion
}
