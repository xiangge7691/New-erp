package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 成品出库台账表
 * <p>
 * 记录成品出库的记账台账（仅记账），成品出库开单后自动联动出库管理完成成品出库
 * </p>
 *
 * @TableName sales_order
 */
@TableName(value = "sales_order")
@Data
@EqualsAndHashCode(callSuper = true)
public class SalesOrder extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 成品出库台账唯一标识
     */
    @TableId(value = "sales_order_id", type = IdType.AUTO)
    private Long salesOrderId;

    /**
     * 成品出库单号（唯一，格式CPCK-YYYYMMDD-NNN）
     */
    @TableField(value = "sales_order_code")
    private String salesOrderCode;

    /**
     * 成品出库日期
     */
    @TableField(value = "sales_order_date")
    private LocalDateTime salesOrderDate;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 客户ID（外键 → customer表）
     */
    @TableField(value = "customer_id")
    private Long customerId;

    /**
     * 客户名称（查询时从customer表回填，非表字段）
     */
    @TableField(exist = false)
    private String customerName;

    /**
     * 制剂名称
     */
    @TableField(value = "preparation_name")
    private String preparationName;

    /**
     * 制剂编码（随制剂自动带出，不可修改）
     */
    @TableField(value = "preparation_code")
    private String preparationCode;

    /**
     * 批号（出库时确定）
     */
    @TableField(value = "batch_number")
    private String batchNumber;

    /**
     * 数量
     */
    @TableField(value = "quantity")
    private Integer quantity;

    /**
     * 单价（默认取制剂信息中的价格，可修改）
     */
    @TableField(value = "unit_price")
    private BigDecimal unitPrice;

    /**
     * 金额（自动计算：数量×单价）
     */
    @TableField(value = "amount")
    private BigDecimal amount;

    /**
     * 关联系统出库单号（回写）
     */
    @TableField(value = "related_out_code")
    private String relatedOutCode;

    /**
     * 状态：已开单/已出库/已作废
     */
    @TableField(value = "status")
    private String status;

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

    // endregion
}
