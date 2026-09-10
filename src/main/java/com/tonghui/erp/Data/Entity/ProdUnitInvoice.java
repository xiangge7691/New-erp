package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产单位发票信息表
 * @TableName prod_unit_invoice
 */
@TableName(value ="prod_unit_invoice")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProdUnitInvoice extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 发票信息唯一标识
     */
    @TableId(value = "prod_invoice_id", type = IdType.AUTO)
    private Long prodInvoiceId;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 关联的生产单位ID
     */
    @TableField(value = "prod_unit_id")
    private Long prodUnitId;

    /**
     * 发票信息内容
     */
    @TableField(value = "prod_invoice_info")
    private String prodInvoiceInfo;

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
