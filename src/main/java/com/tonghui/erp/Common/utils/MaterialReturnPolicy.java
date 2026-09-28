package com.tonghui.erp.Common.utils;

/**
 * 物料退货管理退货来源策略
 * <p>
 * 承载物料退货管理页"退货来源"维度的词表与判定规则：
 * <ul>
 *   <li>退货来源：采购退货 / 领料退货，由退货明细所属验收单的来源类型(sourceType)派生</li>
 *   <li>采购来源：采购入库、退货重发（采购验收不合格产生的退货，或重新发货后再次产生）</li>
 *   <li>领料来源：领料入库（领料单自动生成的验收单，领料验收不合格产生的退货）</li>
 * </ul>
 * 采购退货与领料退货共用同一条退货数据流（acceptance_detail），仅来源不同，操作完全一致
 * </p>
 */
public final class MaterialReturnPolicy {

    // region 退货来源词表
    // ===================================
    // 退货来源词表
    // ===================================

    /** 退货来源：采购退货（验收单来源类型为 采购入库/退货重发） */
    public static final String RETURN_SOURCE_PURCHASE = "采购退货";

    /** 退货来源：领料退货（验收单来源类型为 领料入库） */
    public static final String RETURN_SOURCE_REQUISITION = "领料退货";

    // endregion

    // region 来源判定
    // ===================================
    // 来源判定
    // ===================================

    /**
     * 判断验收单来源类型是否属于"领料退货"来源
     * <p>当前领料入库对应"领料"字样；未来若出现其他领料来源值，含"领料"即判定为领料来源，兼容兜底</p>
     *
     * @param sourceType 验收单来源类型（如 采购入库/退货重发/领料入库）
     * @return 属于领料来源返回 true
     */
    public static boolean isRequisitionSource(String sourceType) {
        return sourceType != null && sourceType.contains("领料");
    }

    // endregion
}