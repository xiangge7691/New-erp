package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 供应商审核记录表
 * @TableName supplier_audit
 */
@TableName(value = "supplier_audit")
@Data
@EqualsAndHashCode(callSuper = true)
public class SupplierAudit extends AuditEntity {

    // region 主键字段
    // ===================================
    // 主键字段
    // ===================================

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // endregion

    // region 供应商关联字段
    // ===================================
    // 供应商关联字段
    // ===================================

    /**
     * 关联供应商ID
     */
    @TableField(value = "supplier_id")
    private Long supplierId;

    /**
     * 供应商名称（冗余字段，便于查询显示）
     */
    @TableField(value = "supplier_name")
    private String supplierName;

    /**
     * 供应类型（原料/辅料/包材/其他）
     */
    @TableField(value = "supply_type")
    private String supplyType;

    // endregion

    // region 审核信息字段
    // ===================================
    // 审核信息字段
    // ===================================

    /**
     * 审核日期
     */
    @TableField(value = "audit_date")
    private Date auditDate;

    /**
     * 审核内容
     */
    @TableField(value = "audit_content")
    private String auditContent;

    /**
     * 审核结果（合格/基本合格/不合格）
     */
    @TableField(value = "audit_result")
    private String auditResult;

    /**
     * 审核人
     */
    @TableField(value = "auditor")
    private String auditor;

    // endregion

    // region 周期管理字段
    // ===================================
    // 周期管理字段
    // ===================================

    /**
     * 审核周期（月）
     */
    @TableField(value = "audit_cycle")
    private Integer auditCycle;

    /**
     * 下次审核日期
     */
    @TableField(value = "next_audit_date")
    private Date nextAuditDate;

    // endregion

    // region 备注字段
    // ===================================
    // 备注字段
    // ===================================

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
