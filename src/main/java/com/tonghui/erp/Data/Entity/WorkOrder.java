package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 工单表
 * @TableName work_order
 */
@TableName(value ="work_order")
@Data
public class WorkOrder {
    /**
     * 工单唯一标识
     */
    @TableId(value = "work_order_id", type = IdType.AUTO)
    private Long workOrderId;

    /**
     * 工单编号（唯一性约束）
     */
    @TableField(value = "work_order_code")
    private String workOrderCode;

    /**
     * 工单名称
     */
    @TableField(value = "work_order_name")
    private String workOrderName;

    /**
     * 制剂ID
     */
    @TableField(value = "preparation_id")
    private Long preparationId;

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
     * 批量
     */
    @TableField(value = "batch_qty")
    private BigDecimal batchQty;

    /**
     * 生产单位
     */
    @TableField(value = "producer")
    private String producer;

    /**
     * 收货单位
     */
    @TableField(value = "receiver")
    private String receiver;

    /**
     * 交付时间
     */
    @TableField(value = "delivery_time")
    private LocalDateTime deliveryTime;

    /**
     * 开票单价
     */
    @TableField(value = "invoice_price")
    private BigDecimal invoicePrice;

    /**
     * 医保单价
     */
    @TableField(value = "insurance_price")
    private BigDecimal insurancePrice;

    /**
     * 结算单价
     */
    @TableField(value = "settlement_price")
    private BigDecimal settlementPrice;

    /**
     * 批号
     */
    @TableField(value = "batch_number")
    private String batchNumber;

    /**
     * 出库量
     */
    @TableField(value = "outbound_qty")
    private BigDecimal outboundQty;

    /**
     * 收款金额
     */
    @TableField(value = "receipt_amount")
    private BigDecimal receiptAmount;

    /**
     * 实收款
     */
    @TableField(value = "actual_receipt_amount")
    private BigDecimal actualReceiptAmount;

    /**
     * 开票金额
     */
    @TableField(value = "invoice_amount")
    private BigDecimal invoiceAmount;

    /**
     * 结算金额
     */
    @TableField(value = "settlement_amount")
    private BigDecimal settlementAmount;

    /**
     * 返款金额
     */
    @TableField(value = "return_amount")
    private BigDecimal returnAmount;

    /**
     * 创建人ID
     */
    @TableField(value = "created_by")
    private Long createdBy;

    /**
     * 更新人ID
     */
    @TableField(value = "updated_by")
    private Long updatedBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time")
    private LocalDateTime updatedTime;
}