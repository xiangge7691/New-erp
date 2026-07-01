package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 库存交易记录表（用于审计和追溯）
 * @TableName stock_transaction
 */
@TableName(value ="stock_transaction")
@Data
public class StockTransaction {
    /**
     * 交易记录唯一标识
     */
    @TableId(value = "transaction_id", type = IdType.AUTO)
    private Long transactionId;

    /**
     * 关联库存ID
     */
    @TableField(value = "stock_id")
    private Long stockId;

    /**
     * 交易类型：入库/出库/调整
     */
    @TableField(value = "transaction_type")
    private Object transactionType;

    /**
     * 交易时间
     */
    @TableField(value = "transaction_date")
    private LocalDateTime transactionDate;

    /**
     * 关联单据ID（入库单ID/出库单ID）
     */
    @TableField(value = "related_id")
    private Long relatedId;

    /**
     * 关联单据类型
     */
    @TableField(value = "related_type")
    private Object relatedType;

    /**
     * 交易前数量
     */
    @TableField(value = "quantity_before")
    private BigDecimal quantityBefore;

    /**
     * 变动数量（正数表示增加，负数表示减少）
     */
    @TableField(value = "quantity_change")
    private BigDecimal quantityChange;

    /**
     * 交易后数量
     */
    @TableField(value = "quantity_after")
    private BigDecimal quantityAfter;

    /**
     * 批次号
     */
    @TableField(value = "batch_number")
    private String batchNumber;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 操作人ID
     */
    @TableField(value = "created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

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