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
 * 领料申请表
 * @TableName material_requisition
 */
@TableName(value = "material_requisition")
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialRequisition extends AuditEntity {

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
     * 领料编号
     */
    @TableField(value = "requisition_code")
    private String requisitionCode;

    /**
     * 关联工单ID
     */
    @TableField(value = "work_order_id")
    private Long workOrderId;

    /**
     * 领料日期
     */
    @TableField(value = "requisition_date")
    private LocalDate requisitionDate;

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
     * 出库种类（原料/辅料/包材）
     */
    @TableField(value = "material_type")
    private String materialType;

    /**
     * 处方倍数
     */
    @TableField(value = "multiplier")
    private BigDecimal multiplier;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 状态（草稿/已提交/已审批/已出库）
     */
    @TableField(value = "status")
    private String status;

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
