package com.tonghui.erp.Common.Dto.Purchase;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购数据导出行数据传输对象
 * <p>
 * 每行对应一条采购订单明细，包含采购订单主表冗余信息、明细物料信息与金额信息，
 * 供采购数据 Excel 导出使用（仅采购数据，不含领料数据）
 * </p>
 */
@Data
public class PurchaseExportRowDto {

    // region 采购订单主表信息
    // ===================================
    // 采购订单主表信息
    // ===================================

    /**
     * 采购订单编号
     */
    private String purchaseNumber;

    /**
     * 生产计划编号
     */
    private String productionPlanCode;

    /**
     * 工单标题
     */
    private String title;

    /**
     * 制剂名称
     */
    private String preparationName;

    /**
     * 批量
     */
    private BigDecimal batchQty;

    /**
     * 处方倍数
     */
    private BigDecimal prescriptionMultiple;

    /**
     * 制剂规格
     */
    private String spec;

    /**
     * 采购订单状态
     */
    private String orderStatus;

    /**
     * 创建时间（采购时间）
     */
    private LocalDateTime createdTime;

    // endregion

    // region 采购明细信息
    // ===================================
    // 采购明细信息
    // ===================================

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 标准处方（标准处方量）
     */
    private BigDecimal standardDosage;

    /**
     * 采购数量
     */
    private BigDecimal purchaseQuantity;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 总价（金额）
     */
    private BigDecimal amount;

    /**
     * 标准量差值
     */
    private BigDecimal difference;

    /**
     * 发票号
     */
    private String invoiceNo;

    /**
     * 供应商
     */
    private String supplier;

    // endregion
}
