package com.tonghui.erp.Common.Dto.Purchase;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 年度采购统计·报表行DTO
 * <p>
 * Excel 报表数据行：物料基础信息 + 采购/领料并列汇总列，
 * 按 material_id 合并采购侧与领料侧聚合结果后生成
 * </p>
 */
@Data
public class PurchaseAnnualStatRowDto {

    // region 物料基础信息
    // ===================================
    // 物料基础信息
    // ===================================

    /**
     * 物料ID
     */
    private Long materialId;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 规格
     */
    private String spec;

    /**
     * 计量单位
     */
    private String unitName;

    /**
     * 物料分类（原料/辅料/包材，决定归属sheet）
     */
    private String categoryName;

    // endregion

    // region 采购侧汇总
    // ===================================
    // 采购侧汇总
    // ===================================

    /**
     * 采购数量合计（口径：actual_arrival_qty，空回退 purchase_quantity）
     */
    private BigDecimal purchaseQty;

    /**
     * 采购金额合计（purchase_order_items.amount）
     */
    private BigDecimal purchaseAmount;

    // endregion

    // region 领料侧汇总
    // ===================================
    // 领料侧汇总
    // ===================================

    /**
     * 领料数量合计（口径：actual_qty，空回退 apply_qty）
     */
    private BigDecimal requisitionQty;

    /**
     * 领料金额合计（数量*单价）
     */
    private BigDecimal requisitionAmount;

    // endregion
}