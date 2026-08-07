package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 采购订单明细表
 * @TableName purchase_order_items
 */
@TableName(value ="purchase_order_items")
@Data
public class PurchaseOrderItems {
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
     * 关联订单ID
     */
    @TableField(value = "order_id")
    private Long orderId;

    /**
     * 序号
     */
    @TableField(value = "sequence_number")
    private Integer sequenceNumber;

    /**
     * 物料ID
     */
    @TableField(value = "material_id")
    private Long materialId;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 制剂名称
     */
    @TableField(value = "product_name")
    private String productName;

    /**
     * 原药材品名
     */
    @TableField(value = "raw_material_name")
    private String rawMaterialName;

    /**
     * 原药材剂量
     */
    @TableField(value = "dose")
    private BigDecimal dose;

    /**
     * 单位ID
     */
    @TableField(value = "unit_id")
    private Long unitId;

    /**
     * 单位
     */
    @TableField(value = "unit")
    private String unit;

    /**
     * 加工性质
     */
    @TableField(value = "processing_property")
    private String processingProperty;

    /**
     * 库存
     */
    @TableField(value = "stock")
    private BigDecimal stock;

    /**
     * 标准处方量
     */
    @TableField(value = "standard_dosage")
    private BigDecimal standardDosage;

    /**
     * 采购数量
     */
    @TableField(value = "purchase_quantity")
    private BigDecimal purchaseQuantity;

    /**
     * 实际到货数量（供应商实际送达数量，金额以该数量为准计算）
     */
    @TableField(value = "actual_arrival_qty")
    private BigDecimal actualArrivalQty;

    /**
     * 单价
     */
    @TableField(value = "unit_price")
    private BigDecimal unitPrice;

    /**
     * 金额（实际到货数量*单价）
     */
    @TableField(value = "amount")
    private BigDecimal amount;

    /**
     * 发票号
     */
    @TableField(value = "invoice_no")
    private String invoiceNo;

    /**
     * 供应商
     */
    @TableField(value = "supplier")
    private String supplier;

    /**
     * 差值
     */
    @TableField(value = "difference")
    private BigDecimal difference;

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
