package com.tonghui.erp.Common.utils;

/**
 * 产品汇总“物料来源”策略
 * <p>
 * 物料来源表示批次所用物料的取得渠道，业务上二选一：采购 / 院内领料；
 * 既无采购也无院内领料、但存在物料直接入库时，兼容保留“直接入库”。
 * 判定依据只看采购单、医院领料单与验收单来源类型；
 * 生产领料单是每批生产都会发生的仓内发料流程，不代表物料取得渠道，不参与判定。
 * </p>
 */
public final class MaterialSourcePolicy {

    // region 物料来源词表
    // ===================================
    // 物料来源词表
    // ===================================

    /** 物料来源：采购（外部采购，含采购退货后重新发货） */
    public static final String PURCHASE = "PURCHASE";

    /** 物料来源：院内领料（制剂室向医院药剂科领料） */
    public static final String REQUISITION = "REQUISITION";

    /** 物料来源：直接入库（无采购、无院内领料，物料直接入库，兼容保留） */
    public static final String DIRECT = "DIRECT";

    /** 物料来源显示文案：采购 */
    private static final String LABEL_PURCHASE = "采购";

    /** 物料来源显示文案：院内领料 */
    private static final String LABEL_REQUISITION = "院内领料";

    /** 物料来源显示文案：直接入库 */
    private static final String LABEL_DIRECT = "直接入库";

    private MaterialSourcePolicy() {
    }

    // endregion

    // region 来源判定
    // ===================================
    // 来源判定
    // ===================================

    /**
     * 判断验收单来源类型是否属于采购来源
     * <p>采购入库：采购订单验收生成；退货重发：采购退货后重新发货生成</p>
     *
     * @param sourceType 验收单来源类型
     * @return 属于采购来源返回 true
     */
    public static boolean isPurchaseSource(String sourceType) {
        return "采购入库".equals(sourceType) || "退货重发".equals(sourceType);
    }

    /**
     * 判断验收单来源类型是否属于院内领料来源
     *
     * @param sourceType 验收单来源类型（如 领料入库）
     * @return 属于院内领料来源返回 true
     */
    public static boolean isRequisitionSource(String sourceType) {
        return MaterialReturnPolicy.isRequisitionSource(sourceType);
    }

    // endregion

    // region 来源汇总
    // ===================================
    // 来源汇总
    // ===================================

    /**
     * 汇总物料来源：采购与院内领料二选一
     * <p>采购证据优先；无采购时为院内领料；两者都无但存在物料入库时返回直接入库；均无证据返回 null</p>
     *
     * @param hasPurchase       是否存在采购证据
     * @param hasRequisition    是否存在院内领料证据
     * @param hasMaterialInbound 是否存在物料入库
     * @return 物料来源编码
     */
    public static String resolve(boolean hasPurchase, boolean hasRequisition, boolean hasMaterialInbound) {
        if (hasPurchase) {
            return PURCHASE;
        }
        if (hasRequisition) {
            return REQUISITION;
        }
        return hasMaterialInbound ? DIRECT : null;
    }

    // endregion

    // region 展示文案
    // ===================================
    // 展示文案
    // ===================================

    /**
     * 物料来源编码转显示文案
     *
     * @param materialSource 物料来源编码
     * @return 显示文案；未知编码原样返回，空值返回 null
     */
    public static String label(String materialSource) {
        if (materialSource == null) {
            return null;
        }
        return switch (materialSource) {
            case PURCHASE -> LABEL_PURCHASE;
            case REQUISITION -> LABEL_REQUISITION;
            case DIRECT -> LABEL_DIRECT;
            default -> materialSource;
        };
    }

    // endregion
}
