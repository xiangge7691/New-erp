package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 出库主表
 * @TableName stock_out
 */
@TableName(value ="stock_out")
@Data
@EqualsAndHashCode(callSuper = true)
public class StockOut extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 出库单唯一标识
     */
    @TableId(value = "out_id", type = IdType.AUTO)
    private Long outId;

    /**
     * 出库单号（唯一）
     */
    @TableField(value = "out_code")
    private String outCode;

    /**
     * 出库类型：生产领料出库/销售出库/报损出库（对齐前端出库管理页面枚举）
     */
    @TableField(value = "out_type")
    private String outType;

    /**
     * 出库仓库（生产单位ID）
     */
    @TableField(value = "prod_unit_id")
    private Long prodUnitId;

    /**
     * 客户ID（如果是销售出库）
     */
    @TableField(value = "customer_id")
    private Long customerId;

    /**
     * 关联单号（销售订单号、生产任务单等）
     */
    @TableField(value = "related_order")
    private String relatedOrder;

    /**
     * 关联生产计划ID（production_plan.id）
     */
    @TableField(value = "plan_id")
    private Long planId;

    /**
     * 关联生产计划编号（production_plan.plan_number）
     */
    @TableField(value = "plan_number")
    private String planNumber;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 出库日期
     */
    @TableField(value = "out_date")
    private LocalDate outDate;

    /**
     * 出库总金额
     */
    @TableField(value = "total_amount")
    private BigDecimal totalAmount;

    /**
     * 状态：草稿/已确认/已完成/已取消
     */
    @TableField(value = "out_status")
    private String outStatus;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 审批实例ID
     */
    @TableField(value = "approval_instance_id")
    private Long approvalInstanceId;

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
