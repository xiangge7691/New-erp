package com.tonghui.erp.Common.Dto.Purchase;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 年度采购统计·聚合查询结果DTO
 * <p>
 * 采购侧（purchase_order_items 聚合）与领料侧（material_requisition_slip_detail 聚合）
 * 共用的按物料分组汇总结构
 * </p>
 */
@Data
public class PurchaseAnnualAggDto {

    // region 汇总字段
    // ===================================
    // 汇总字段
    // ===================================

    /**
     * 物料ID（关联material表取分类）
     */
    private Long materialId;

    /**
     * 物料编码（materialId缺失时作为合并回退键）
     */
    private String materialCode;

    /**
     * 数量合计（采购侧=到货量，领料侧=实发量，空回退）
     */
    private BigDecimal quantity;

    /**
     * 金额合计（采购侧=订单明细金额，领料侧=数量*单价）
     */
    private BigDecimal amount;

    // endregion
}