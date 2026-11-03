package com.tonghui.erp.Common.Constants;

/**
 * 采购流程节点枚举
 * <p>
 * 定义生产计划关联采购流程的六个标准节点，节点顺序同时作为时间线排序依据。
 * 采购申请时间复用 purchase_plan.created_time，入库时间复用 stock_in.in_date，
 * 其余节点时间由采购计划、采购订单、验收单上的业务时间字段承载。
 * </p>
 */
public enum PurchaseProcessNode {

    // region 节点定义
    // ===================================
    // 节点定义
    // ===================================

    /** 采购申请 */
    APPLY("APPLY", "采购申请", 0),

    /** 审批 */
    APPROVE("APPROVE", "审批", 1),

    /** 下单 */
    ORDER("ORDER", "下单", 2),

    /** 到货 */
    ARRIVE("ARRIVE", "到货", 3),

    /** 验收 */
    ACCEPT("ACCEPT", "验收", 4),

    /** 入库 */
    INBOUND("INBOUND", "入库", 5),

    /** 异常（驳回、作废等，不参与正常节点进度计算） */
    ABNORMAL("ABNORMAL", "异常", 6);

    // endregion

    // region 属性与构造方法
    // ===================================
    // 属性与构造方法
    // ===================================

    /** 节点编码 */
    private final String code;

    /** 节点名称 */
    private final String name;

    /** 节点顺序索引，从 0 开始 */
    private final int index;

    PurchaseProcessNode(String code, String name, int index) {
        this.code = code;
        this.name = name;
        this.index = index;
    }

    // endregion

    // region 公共方法
    // ===================================
    // 公共方法
    // ===================================

    /**
     * 根据节点编码解析枚举
     *
     * @param code 节点编码
     * @return 对应枚举，未匹配时返回 null
     */
    public static PurchaseProcessNode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PurchaseProcessNode node : values()) {
            if (node.code.equals(code)) {
                return node;
            }
        }
        return null;
    }

    /**
     * 计算节点进度百分比
     * <p>按六个业务节点均分，当前节点未完成时只计算其之前的节点。</p>
     *
     * @param completedCount 已完成节点数
     * @return 0~100 的进度百分比
     */
    public static int progressPercent(int completedCount) {
        int safeCompleted = Math.max(0, Math.min(6, completedCount));
        return safeCompleted * 100 / 6;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    // endregion
}
