package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 货物验收表导出行数据传输对象
 * <p>
 * 一行对应一条验收明细（acceptance_detail），同时冗余其所属验收单（acceptance_order）的主单字段，
 * 用于货物验收表 Excel 的明细平铺导出
 * </p>
 */
@Data
public class AcceptanceExportRowDto {

    // region 验收单主单字段（明细行冗余）
    // ===================================
    // 验收单主单字段（明细行冗余）
    // ===================================

    /**
     * 验收单号（acceptance_order.acceptance_code）
     */
    private String acceptanceCode;

    /**
     * 来源类型：采购入库/成品入库/直接入库/退货重发（acceptance_order.source_type）
     */
    private String sourceType;

    /**
     * 关联采购订单号（acceptance_order.related_order）
     */
    private String relatedOrder;

    /**
     * 关联采购计划编号（acceptance_order.purchase_number）
     */
    private String purchaseNumber;

    /**
     * 收货单位名称（acceptance_order.unit_name）
     */
    private String unitName;

    /**
     * 验收单状态（acceptance_order.status）
     */
    private String status;

    /**
     * 交期/预计交付日期（acceptance_order.delivery_date）
     */
    private LocalDate deliveryDate;

    /**
     * 备注（acceptance_order.remark）
     */
    private String remark;

    // endregion

    // region 验收明细字段
    // ===================================
    // 验收明细字段
    // ===================================

    /**
     * 明细序号（acceptance_detail.seq）
     */
    private Integer seq;

    /**
     * 物料编码（acceptance_detail.material_code）
     */
    private String materialCode;

    /**
     * 物料名称（acceptance_detail.material_name）
     */
    private String materialName;

    /**
     * 物料分类：原料/辅料/包材/成品（acceptance_detail.material_category）
     */
    private String materialCategory;

    /**
     * 计量单位（acceptance_detail.unit_name）
     */
    private String materialUnit;

    /**
     * 采购数量（acceptance_detail.quantity）
     */
    private BigDecimal quantity;

    /**
     * 实际到货数量（acceptance_detail.actual_arrival_qty）
     */
    private BigDecimal actualArrivalQty;

    /**
     * 入库数量（acceptance_detail.inbound_qty）
     */
    private BigDecimal inboundQty;

    /**
     * 物料单价（acceptance_detail.unit_price）
     */
    private BigDecimal unitPrice;

    /**
     * 金额（acceptance_detail.amount）
     */
    private BigDecimal amount;

    /**
     * 物料批号（acceptance_detail.batch_number）
     */
    private String batchNumber;

    /**
     * 有效期至（acceptance_detail.expiry_date）
     */
    private LocalDate expiryDate;

    /**
     * 明细状态（acceptance_detail.status）
     */
    private String detailStatus;

    /**
     * 退货原因（acceptance_detail.return_reason）
     */
    private String returnReason;

    /**
     * 不合格原因说明（acceptance_detail.return_remark）
     */
    private String returnRemark;

    /**
     * 供应商（acceptance_detail.supplier）
     */
    private String supplier;

    // endregion
}
