package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 采购订单主表
 * @TableName purchase_orders
 */
@TableName(value ="purchase_orders")
@Data
public class PurchaseOrders {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 采购编号
     */
    @TableField(value = "purchase_number")
    private String purchaseNumber;

    /**
     * 供应商ID
     */
    @TableField(value = "supplier_id")
    private Long supplierId;

    /**
     * 仓库（生产单位ID）
     */
    @TableField(value = "prod_unit_id")
    private Long prodUnitId;

    /**
     * 仓库
     */
    @TableField(value = "warehouse")
    private String warehouse;

    /**
     * 处理日期
     */
    @TableField(value = "processing_date")
    private LocalDate processingDate;

    /**
     * 预计到货日期
     */
    @TableField(value = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    /**
     * 发票信息
     */
    @TableField(value = "invoice_info")
    private String invoiceInfo;

    /**
     * 收货信息
     */
    @TableField(value = "receiving_info")
    private String receivingInfo;

    /**
     * 制剂所属单位
     */
    @TableField(value = "unit")
    private String unit;

    /**
     * 采购单标题
     */
    @TableField(value = "title")
    private String title;

    /**
     * 处方倍数
     */
    @TableField(value = "prescription_multiple")
    private BigDecimal prescriptionMultiple;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 是否生成生产计划
     */
    @TableField(value = "generate_production_plan")
    private Integer generateProductionPlan;

    /**
     * 状态
     */
    @TableField(value = "status")
    private Object status;

    /**
     * 审批实例ID
     */
    @TableField(value = "approval_instance_id")
    private Long approvalInstanceId;

    /**
     * 创建人ID
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    private Long createdBy;

    /**
     * 更新人ID
     */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

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
}