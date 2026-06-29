package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 采购订单主表
 * @TableName purchase_orders
 */
@TableName(value ="purchase_orders")
@Data
public class PurchaseOrders {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 采购编号
     */
    @TableField(value = "purchase_number")
    private String purchaseNumber;

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
     * 预计到货日期
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
     * 处方倍数
     */
    @TableField(value = "prescription_multiple")
    private BigDecimal prescriptionMultiple;

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
     * 状态
     */
    @TableField(value = "status")
    private Object status;
}