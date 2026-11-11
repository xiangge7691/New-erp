package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Data.Entity.WorkOrder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态策略
 * <p>
 * 集中承载工单当前状态词表、状态推导与“生产完成/在产”判定，避免状态字面量散落在各业务中：
 * <ul>
 *   <li>状态流转：待生产 → 生产中 → 已生产 → 检验中 → 已检验 → 已入库 → 已放行 → 已归档；
 *       实际业务链路为 检验 → 成品入库 → 审核放行 → 成品出库，因此成品入库后通过审核放行会把状态推进到“已放行”</li>
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

    /** 已入库（成品入库时间有值，生产完成） */
    public static final String INBOUND = "已入库";

    /** 已放行（审核放行时间有值，成品入库后的后续环节） */
    public static final String RELEASED = "已放行";

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

    /** 状态先后顺序，用于合并“库内状态”和“按日期推导状态”时取更靠后的一方 */
    private static final Map<String, Integer> STATUS_RANK = Map.of(
            PENDING, 0,
            PRODUCING, 1,
            PRODUCED, 2,
            INSPECTING, 3,
            INSPECTED, 4,
            INBOUND, 5,
            RELEASED, 6,
            ARCHIVED, 7);

    // endregion

    // region 判定规则
    // ===================================
    // 判定规则
    // ===================================

    /** 生产已完成状态：成品已入库及其后续环节 */
    private static final Set<String> PRODUCTION_DONE_STATUSES = Set.of(INBOUND, RELEASED, ARCHIVED, COMPLETED);

    /** 终止状态：异常/不合格/作废，既不算在产也不算生产完成 */
    private static final Set<String> TERMINAL_STATUSES = Set.of(ABNORMAL, UNQUALIFIED, VOIDED);

    /** 非在产状态：生产完成及各类终止状态 */
    private static final Set<String> NON_RUNNING_STATUSES = Set.of(
            INBOUND, RELEASED, ARCHIVED, COMPLETED, ABNORMAL, UNQUALIFIED, VOIDED);

    /**
     * 按时间字段推导工单状态
     * <p>
     * 状态流转规则（依据配置日期、配置完成日期、检验、放行、入库、归档时间）：
     * <ul>
     *   <li>待生产 - 配置日期为空</li>
     *   <li>生产中 - 配置日期有值 且 配置完成日期为空</li>
     *   <li>已生产 - 配置完成日期有值</li>
     *   <li>检验中 - 检验开始时间有值</li>
     *   <li>已检验 - 检验完成时间有值</li>
     *   <li>已入库 - 入库时间有值</li>
     *   <li>已放行 - 审核放行时间有值（业务顺序在成品入库之后）</li>
     *   <li>已归档 - 归档时间有值</li>
     * </ul>
     * </p>
     *
     * @param configDate         配置日期
     * @param configCompleteTime 配置完成日期
     * @param archiveTime        归档时间
     * @param inspectionStart    检验开始时间
     * @param inspectionEnd      检验完成时间
     * @param auditReleaseTime   审核放行时间
     * @param inboundTime        入库时间
     * @return 计算后的工单状态
     */
    public static String resolve(LocalDateTime configDate, LocalDateTime configCompleteTime,
                                 LocalDateTime archiveTime, LocalDateTime inspectionStart,
                                 LocalDateTime inspectionEnd, LocalDateTime auditReleaseTime,
                                 LocalDateTime inboundTime) {
        if (archiveTime != null) {
            return ARCHIVED;
        }
        if (auditReleaseTime != null) {
            return RELEASED;
        }
        if (inboundTime != null) {
            return INBOUND;
        }
        if (inspectionEnd != null) {
            return INSPECTED;
        }
        if (inspectionStart != null) {
            return INSPECTING;
        }
        if (configCompleteTime != null) {
            return PRODUCED;
        }
        if (configDate != null) {
            return PRODUCING;
        }
        return PENDING;
    }

    /**
     * 推导工单的有效状态
     * <p>
     * 终止状态（异常/不合格/作废）以库内值为准；其余状态按时间字段实时推导，
     * 并与库内状态取更靠后的一个，避免历史数据未回写导致页面状态滞后
     * （例如已审核放行但库内仍是“已入库”）。
     * </p>
     *
     * @param workOrder 工单
     * @return 有效状态；工单为空时返回 null
     */
    public static String effectiveStatus(WorkOrder workOrder) {
        if (workOrder == null) {
            return null;
        }
        String stored = workOrder.getCurrentStatus();
        if (stored != null && TERMINAL_STATUSES.contains(stored)) {
            return stored;
        }
        String derived = resolve(workOrder.getConfigDate(), workOrder.getConfigCompleteTime(),
                workOrder.getArchiveTime(), workOrder.getInspectionStart(), workOrder.getInspectionEnd(),
                workOrder.getAuditReleaseTime(), workOrder.getInboundTime());
        Integer storedRank = STATUS_RANK.get(stored);
        Integer derivedRank = STATUS_RANK.get(derived);
        if (storedRank != null && derivedRank != null && storedRank > derivedRank) {
            return stored;
        }
        return derived;
    }

    /**
     * 判断工单生产是否已完成
     * <p>成品入库即视为生产完成，其后的审核放行、已归档及历史“已完成”同样视为完成。</p>
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

    /**
     * 判断是否为终止状态（异常/不合格/作废）
     *
     * @param status 工单当前状态
     * @return 终止状态返回 true
     */
    public static boolean isTerminal(String status) {
        return status != null && TERMINAL_STATUSES.contains(status);
    }

    // endregion

    private WorkOrderStatusPolicy() {
    }
}
