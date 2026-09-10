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
 * 采购订单主表
 * @TableName purchase_orders
 */
@TableName(value ="purchase_orders")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrders extends AuditEntity {
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

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 处理日期
     */
    @TableField(value = "processing_date")
    private LocalDate processingDate;

    /**
     * 期望到货日期（采购方期望的到货时间）
     */
    @TableField(value = "desired_delivery_date")
    private LocalDate desiredDeliveryDate;

    /**
     * 预计到货日期（供应商预计的到货时间）
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
     * 关联采购计划ID
     */
    @TableField(value = "plan_id")
    private Long planId;

    /**
     * 采购计划编号
     */
    @TableField(value = "plan_code")
    private String planCode;

    /**
     * 关联生产计划ID
     */
    @TableField(value = "production_plan_id")
    private Long productionPlanId;

    /**
     * 生产计划编号
     */
    @TableField(value = "production_plan_code")
    private String productionPlanCode;

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
     * 规格
     */
    @TableField(value = "spec")
    private String spec;

    /**
     * 批量
     */
    @TableField(value = "batch_qty")
    private BigDecimal batchQty;

    /**
     * 处方倍数
     */
    @TableField(value = "prescription_multiple")
    private BigDecimal prescriptionMultiple;

    /**
     * 物料类型（原料/辅料/包材）
     */
    @TableField(value = "material_type")
    private String materialType;

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
     * 订单状态
     * <p>状态流转：待采购 → 运输中 → 到货初验 → 物料检验 → 已入库/待退货 → 已关闭</p>
     */
    @TableField(value = "status")
    private Object status;

    /**
     * 审批意见
     */
    @TableField(value = "approval_opinion")
    private String approvalOpinion;

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
