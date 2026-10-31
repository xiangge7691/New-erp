package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产领料单主表实体
 * <p>
 * 记录从生产任务发起的领料申请，包含关联生产任务号/任务名、用途、领料人、审核人、发料人，
 * 以及发料后联动生成的“生产领料出库”出库单信息。状态流转：待发料 → 已发料，异常情况可作废。
 * </p>
 *
 * @TableName production_pick
 */
@TableName(value = "production_pick")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductionPick extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 领料单号（格式 SCLL-YYYYMMDD-NNN，系统自动生成）
     */
    @TableField(value = "pick_code")
    private String pickCode;

    /**
     * 关联生产任务ID
     */
    @TableField(value = "work_order_id")
    private Long workOrderId;

    /**
     * 关联生产任务号
     */
    @TableField(value = "work_order_code")
    private String workOrderCode;

    /**
     * 关联生产任务名
     */
    @TableField(value = "work_order_name")
    private String workOrderName;

    /**
     * 关联生产计划ID
     */
    @TableField(value = "plan_id")
    private Long planId;

    /**
     * 关联生产计划编号
     */
    @TableField(value = "plan_number")
    private String planNumber;

    /**
     * 制剂ID
     */
    @TableField(value = "preparation_id")
    private Long preparationId;

    /**
     * 制剂编码
     */
    @TableField(value = "preparation_code")
    private String preparationCode;

    /**
     * 制剂名称
     */
    @TableField(value = "preparation_name")
    private String preparationName;

    /**
     * 生产批号
     */
    @TableField(value = "batch_number")
    private String batchNumber;

    // endregion

    // region 领料信息字段
    // ===================================
    // 领料信息字段
    // ===================================

    /**
     * 领料车间
     */
    @TableField(value = "workshop")
    private String workshop;

    /**
     * 领料时间
     */
    @TableField(value = "pick_time")
    private LocalDateTime pickTime;

    /**
     * 用途（默认按制剂名称+批号生成）
     */
    @TableField(value = "purpose")
    private String purpose;

    /**
     * 领料人
     */
    @TableField(value = "applicant")
    private String applicant;

    /**
     * 审核人
     */
    @TableField(value = "reviewer")
    private String reviewer;

    /**
     * 审核时间
     */
    @TableField(value = "reviewer_time")
    private LocalDateTime reviewerTime;

    /**
     * 发料人
     */
    @TableField(value = "issuer")
    private String issuer;

    /**
     * 发料时间
     */
    @TableField(value = "issue_time")
    private LocalDateTime issueTime;

    /**
     * 处方倍数（按生产任务发起时记录）
     */
    @TableField(value = "multiplier")
    private BigDecimal multiplier;

    // endregion

    // region 状态与出库联动字段
    // ===================================
    // 状态与出库联动字段
    // ===================================

    /**
     * 状态（待发料/已发料/已作废）
     */
    @TableField(value = "status")
    private String status;

    /**
     * 联动生成的出库单ID
     */
    @TableField(value = "stock_out_id")
    private Long stockOutId;

    /**
     * 联动生成的出库单号
     */
    @TableField(value = "stock_out_code")
    private String stockOutCode;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
    // ===================================

    /**
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 乐观锁版本号
     */
    @TableField(value = "version")
    private Integer version;

    // endregion
}
