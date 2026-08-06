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
 * 货物验收单主表
 * @TableName acceptance_order
 */
@TableName(value = "acceptance_order")
@Data
@EqualsAndHashCode(callSuper = true)
public class AcceptanceOrder extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 验收单唯一标识
     */
    @TableId(value = "acceptance_id", type = IdType.AUTO)
    private Long acceptanceId;

    /**
     * 验收单号（唯一，格式 YS-YYYYMMDD-NNN）
     */
    @TableField(value = "acceptance_code")
    private String acceptanceCode;

    /**
     * 来源类型：采购入库/成品入库/直接入库
     */
    @TableField(value = "source_type")
    private String sourceType;

    /**
     * 关联采购订单号
     */
    @TableField(value = "related_order")
    private String relatedOrder;

    /**
     * 关联采购计划编号
     */
    @TableField(value = "purchase_number")
    private String purchaseNumber;

    /**
     * 关联生产计划编号
     */
    @TableField(value = "plan_code")
    private String planCode;

    /**
     * 关联生产任务编号
     */
    @TableField(value = "work_order_code")
    private String workOrderCode;

    /**
     * 生产计划标题
     */
    @TableField(value = "title")
    private String title;

    /**
     * 收货单位名称
     */
    @TableField(value = "unit_name")
    private String unitName;

    // endregion

    // region 制剂关联字段
    // ===================================
    // 制剂关联字段
    // ===================================

    /**
     * 关联制剂编码
     */
    @TableField(value = "preparation_code")
    private String preparationCode;

    /**
     * 关联制剂名称
     */
    @TableField(value = "preparation_name")
    private String preparationName;

    /**
     * 制剂规格
     */
    @TableField(value = "spec")
    private String spec;

    /**
     * 计划生产批量
     */
    @TableField(value = "batch_qty")
    private BigDecimal batchQty;

    /**
     * 处方生产倍数
     */
    @TableField(value = "prescription_multiple")
    private BigDecimal prescriptionMultiple;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 入库仓库（生产单位ID，检验合格时选择）
     */
    @TableField(value = "prod_unit_id")
    private Long prodUnitId;

    /**
     * 状态：运输中/到货初验/物料检验/已入库/待退货/已退换
     */
    @TableField(value = "status")
    private String status;

    /**
     * 预计交付日期（交期）
     */
    @TableField(value = "delivery_date")
    private LocalDate deliveryDate;

    /**
     * 备注（流程节点自动追加）
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 原验收单号（重新收货时记录）
     */
    @TableField(value = "original_acceptance_code")
    private String originalAcceptanceCode;

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
