package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 采购计划表
 * @TableName purchase_plan
 */
@TableName(value = "purchase_plan")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchasePlan extends AuditEntity {

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
     * 采购计划编号
     */
    @TableField(value = "plan_code")
    private String planCode;

    /**
     * 关联生产任务ID
     */
    @TableField(value = "work_order_id")
    private Long workOrderId;

    /**
     * 生产任务编号
     */
    @TableField(value = "work_order_code")
    private String workOrderCode;

    /**
     * 工单标题
     */
    @TableField(value = "title")
    private String title;

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

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

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
     * 收货单位
     */
    @TableField(value = "receiving_unit")
    private String receivingUnit;

    /**
     * 收货地址
     */
    @TableField(value = "receiving_address")
    private String receivingAddress;

    /**
     * 发票信息
     */
    @TableField(value = "invoice_info")
    private String invoiceInfo;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 状态与审批字段
    // ===================================
    // 状态与审批字段
    // ===================================

    /**
     * 状态（草稿/待审批/已审批/已驳回）
     */
    @TableField(value = "status")
    private String status;

    /**
     * 审批意见
     */
    @TableField(value = "approval_opinion")
    private String approvalOpinion;

    /**
     * 关联的采购订单ID
     */
    @TableField(value = "purchase_order_id")
    private Long purchaseOrderId;

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
