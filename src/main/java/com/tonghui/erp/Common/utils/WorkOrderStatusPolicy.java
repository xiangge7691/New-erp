package com.tonghui.erp.Common.utils;

import java.util.Set;

/**
 * 工单状态策略
 * <p>
 * 集中承载工单当前状态词表与“生产完成/在产”判定，避免状态字面量散落在汇总统计中：
 * <ul>
 *   <li>状态流转：待生产 → 生产中 → 已生产 → 检验中 → 已检验 → 已放行 → 已入库 → 已归档</li>
 *   <li>生产视角下成品入库即视为生产完成，审核放行/成品出库属于入库后的后续环节，不计入在产批次</li>
 *   <li>异常/不合格/作废为终止状态，同样不计入在产批次</li>
 * </ul>
 * </p>
 */
public final class WorkOrderStatusPolicy {

    // region 状态词表
    // ===================================
    // 状态词表
    // ===================================

    /** 待生产（配置日期为空） */
    public static final String PENDING = "待生产";

    /** 生产中（配置日期有值、配置完成日期为空） */
    public static final String PRODUCING = "生产中";

    /** 已生产（配置完成日期有值） */
    public static final String PRODUCED = "已生产";

    /** 检验中（检验开始时间有值） */
    public static final String INSPECTING = "检验中";

    /** 已检验（检验完成时间有值） */
    public static final String INSPECTED = "已检验";

    /** 已放行（审核放行时间有值、尚未入库） */
    public static final String RELEASED = "已放行";

    /** 已入库（成品入库时间有值，生产完成） */
    public static final String INBOUND = "已入库";

    /** 已归档（归档时间有值，终态） */
    public static final String ARCHIVED = "已归档";

    /** 已完成（历史/兼容状态，视同生产完成） */
    public static final String COMPLETED = "已完成";

    /** 异常（终止状态） */
    public static final String ABNORMAL = "异常";

    /** 不合格（终止状态） */
    public static final String UNQUALIFIED = "不合格";

    /** 作废（终止状态） */
    public static final String VOIDED = "作废";

    // endregion

    // region 判定规则
    // ===================================
    // 判定规则
    // ===================================

    /** 生产已完成状态：成品已入库或已归档 */
    private static final Set<String> PRODUCTION_DONE_STATUSES = Set.of(INBOUND, ARCHIVED, COMPLETED);

    /** 非在产状态：生产完成及各类终止状态 */
    private static final Set<String> NON_RUNNING_STATUSES = Set.of(
            INBOUND, ARCHIVED, COMPLETED, ABNORMAL, UNQUALIFIED, VOIDED);

    /**
     * 判断工单生产是否已完成
     * <p>成品入库即视为生产完成；已归档、历史“已完成”同样视为完成。</p>
     *
     * @param status 工单当前状态
     * @return 生产已完成返回 true
     */
    public static boolean isProductionDone(String status) {
        return status != null && PRODUCTION_DONE_STATUSES.contains(status);
    }

    /**
     * 判断工单是否仍在产
     * <p>未入库且未进入异常/不合格/作废等终止状态时视为在产；状态为空时按在产兜底。</p>
     *
     * @param status 工单当前状态
     * @return 在产返回 true
     */
    public static boolean isRunning(String status) {
        return status == null || !NON_RUNNING_STATUSES.contains(status);
    }

    // endregion

    private WorkOrderStatusPolicy() {
    }
}
