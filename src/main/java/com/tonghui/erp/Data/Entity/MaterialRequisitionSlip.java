package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物料领料单主表实体
 * <p>
 * 制剂室向医院药剂科申请领料的单据，包含领料单号、关联生产计划、领用/发放科室、领料人、状态等信息。
 * 支持状态流转：待发放 → 验收中 → 已入库，或待发放 → 已作废。
 * </p>
 *
 * @TableName material_requisition_slip
 */
@TableName(value = "material_requisition_slip")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialRequisitionSlip extends AuditEntity {

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
     * 领料单号（格式 LL-YYYYMMDD-NNN，系统自动生成）
     */
    @TableField(value = "slip_code")
    private String slipCode;

    /**
     * 关联生产计划ID（生产计划联动生成时自动带入）
     */
    @TableField(value = "production_plan_id")
    private Long productionPlanId;

    /**
     * 关联生产计划编号
     */
    @TableField(value = "production_plan_code")
    private String productionPlanCode;

    /**
     * 生产计划标题
     */
    @TableField(value = "production_plan_title")
    private String productionPlanTitle;

    // endregion

    // region 领料信息字段
    // ===================================
    // 领料信息字段
    // ===================================

    /**
     * 领用科室（固定值：制剂室）
     */
    @TableField(value = "from_dept")
    private String fromDept;

    /**
     * 发放科室（固定值：医院药剂科）
     */
    @TableField(value = "to_dept")
    private String toDept;

    /**
     * 领料人（制剂室提交人）
     */
    @TableField(value = "applicant")
    private String applicant;

    /**
     * 领料时间
     */
    @TableField(value = "apply_time")
    private LocalDateTime applyTime;

    /**
     * 物料类型（原料/辅料/包材）
     */
    @TableField(value = "material_type")
    private String materialType;

    /**
     * 处方倍数
     */
    @TableField(value = "multiplier")
    private java.math.BigDecimal multiplier;

    // endregion

    // region 状态与关联字段
    // ===================================
    // 状态与关联字段
    // ===================================

    /**
     * 状态（待发放/验收中/已入库/已作废）
     */
    @TableField(value = "status")
    private String status;

    /**
     * 关联货物验收单ID（新增领料单时自动生成）
     */
    @TableField(value = "acceptance_order_id")
    private Long acceptanceOrderId;

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
