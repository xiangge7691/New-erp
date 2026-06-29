package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 生产单位发票信息表
 * @TableName prod_unit_invoice
 */
@TableName(value ="prod_unit_invoice")
@Data
public class ProdUnitInvoice {
    /**
     * 发票信息唯一标识
     */
    @TableId(value = "prod_invoice_id", type = IdType.AUTO)
    private Long prodInvoiceId;

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

    /**
     * 创建人ID
     */
    @TableField(value = "created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

    /**
     * 更新人ID
     */
    @TableField(value = "updated_by")
    private Long updatedBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}