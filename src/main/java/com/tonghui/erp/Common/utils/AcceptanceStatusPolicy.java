package com.tonghui.erp.Common.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 货物验收与采购订单状态策略
 * <p>
 * 集中承载三模块（采购订单/货物验收/物料退货管理）的状态词表、
 * 主单状态派生规则与明细状态流转规则，避免状态字面量散落各处：
 * <ul>
 *   <li>主单状态（验收单与采购订单共享）：运输中/验收中/已入库/已结束/已作废，采购订单另有初始态"待采购"</li>
 *   <li>明细状态（验收明细与采购订单明细共享）：待初验/待检验/待入库/待退货/已退货/已重发/已入库/已取消</li>
 *   <li>主单状态由明细汇总派生，判定优先级：已作废 ＞ 已入库 ＞ 已结束 ＞ 验收中 ＞ 运输中</li>
 *   <li>"存在退货"为派生布尔：任一明细处于 待退货/已退货/已取消 时为 true，不作为主单状态</li>
 * </ul>
 * </p>
 */
public final class AcceptanceStatusPolicy {

    // region 主单状态词表
    // ===================================
    // 主单状态词表
    // ===================================

    /** 主单状态：运输中（验收单创建后/确认采购后的初始状态，尚未确认到货） */
    public static final String MAIN_TRANSPORTING = "运输中";

    /** 主单状态：验收中（已确认到货，逐明细初验/检验中） */
    public static final String MAIN_INSPECTING = "验收中";

    /** 主单状态：已入库（整单入库完成，合格明细已入库存，终态） */
    public static final String MAIN_INBOUND = "已入库";

    /** 主单状态：已结束（所有明细均退货/取消，无待入库明细，终态） */
    public static final String MAIN_FINISHED = "已结束";

    /** 主单状态：已作废（整单作废，终态） */
    public static final String MAIN_VOIDED = "已作废";

    /** 采购订单特有初始状态：待采购（审批通过自动生成，未确认采购） */
    public static final String ORDER_PENDING = "待采购";

    // endregion

    // region 明细状态词表
    // ===================================
    // 明细状态词表
    // ===================================

    /** 明细状态：待初验（已到货待初步检验） */
    public static final String DETAIL_PENDING_INITIAL = "待初验";

    /** 明细状态：待检验（初验合格待质量检验） */
    public static final String DETAIL_PENDING_QUALITY = "待检验";

    /** 明细状态：待入库（检验合格，等待整单入库） */
    public static final String DETAIL_PENDING_INBOUND = "待入库";

    /** 明细状态：待退货（初验/检验不合格，待采购部门在物料退货管理页退货） */
    public static final String DETAIL_PENDING_RETURN = "待退货";

    /** 明细状态：已退货（采购部门已确认退回供应商） */
    public static final String DETAIL_RETURNED = "已退货";

    /** 明细状态：已重发（退货已重新发货，生成新验收单，处理完结） */
    public static final String DETAIL_RESENT = "已重发";

    /** 明细状态：已入库（整单入库完成） */
    public static final String DETAIL_INBOUND = "已入库";

    /** 明细状态：已取消（退货后该物料取消采购） */
    public static final String DETAIL_CANCELLED = "已取消";

    // endregion

    // region 退货原因词表
    // ===================================
    // 退货原因词表
    // ===================================

    /** 退货原因：初验不合格（货物验收初验环节带出，只读） */
    public static final String REASON_INITIAL_UNQUALIFIED = "初验不合格";

    /** 退货原因：检验不合格（货物验收检验环节带出，只读） */
    public static final String REASON_QUALITY_UNQUALIFIED = "检验不合格";

    // endregion

    // region 操作阶段词表
    // ===================================
    // 操作阶段词表
    // ===================================

    /** 操作阶段：初验（明细行处于待初验时可执行初验/部分验收） */
    public static final String STAGE_INITIAL = "初验";

    /** 操作阶段：检验（明细行处于待检验时可执行检验/部分验收） */
    public static final String STAGE_QUALITY = "检验";

    // endregion

    // region 集合定义
    // ===================================
    // 集合定义
    // ===================================

    /** 明细终结状态集合：流程无需在验收页继续推进的状态 */
    private static final Set<String> DETAIL_TERMINAL_STATUSES = Set.of(
            DETAIL_PENDING_INBOUND, DETAIL_PENDING_RETURN, DETAIL_RETURNED,
            DETAIL_RESENT, DETAIL_INBOUND, DETAIL_CANCELLED);

    /** "存在退货"派生集合：任一明细处于以下状态时主单存在退货为 true */
    private static final Set<String> DETAIL_RETURN_STATUSES = Set.of(
            DETAIL_PENDING_RETURN, DETAIL_RETURNED, DETAIL_CANCELLED);

    /** 全部退货/取消类状态：无待入库明细时主单可判"已结束" */
    private static final Set<String> DETAIL_RETURN_LIKE = Set.of(
            DETAIL_PENDING_RETURN, DETAIL_RETURNED, DETAIL_RESENT, DETAIL_CANCELLED);

    /**
     * 明细状态合法流转表（状态只能按此表向下流转，不可回退）
     * <p>对应文档流转图：初验/检验合格推进、不合格进退货、退货页完成退货/重发/取消</p>
     */
    private static final Map<String, Set<String>> DETAIL_TRANSITIONS = Map.of(
            DETAIL_PENDING_INITIAL, Set.of(DETAIL_PENDING_QUALITY, DETAIL_PENDING_RETURN),
            DETAIL_PENDING_QUALITY, Set.of(DETAIL_PENDING_INBOUND, DETAIL_PENDING_RETURN),
            DETAIL_PENDING_INBOUND, Set.of(DETAIL_INBOUND),
            DETAIL_PENDING_RETURN, Set.of(DETAIL_RETURNED),
            DETAIL_RETURNED, Set.of(DETAIL_RESENT, DETAIL_CANCELLED));

    // endregion

    // region 构造与工具方法
    // ===================================
    // 构造与工具方法
    // ===================================

    /**
     * 私有构造，工具类不可实例化
     */
    private AcceptanceStatusPolicy() {
    }

    /**
     * 判断明细状态是否为终结状态
     *
     * @param status 明细状态
     * @return 是否终结（待入库/待退货/已退货/已重发/已入库/已取消）
     */
    public static boolean isDetailTerminal(String status) {
        return status != null && DETAIL_TERMINAL_STATUSES.contains(status);
    }

    /**
     * 判断一组明细状态中是否存在"存在退货"标志
     *
     * @param statuses 明细状态集合
     * @return 存在 待退货/已退货/已取消 的明细时返回 true
     */
    public static boolean hasReturn(Collection<String> statuses) {
        return statuses != null && statuses.stream().anyMatch(DETAIL_RETURN_STATUSES::contains);
    }

    /**
     * 判断明细状态是否允许从 from 流转到 to（只能向下，不可回退）
     *
     * @param from 当前明细状态
     * @param to   目标明细状态
     * @return 流转合法返回 true
     */
    public static boolean isLegalDetailTransition(String from, String to) {
        if (from == null || to == null) {
            return false;
        }
        if (from.equals(to)) {
            return true;
        }
        Set<String> allowed = DETAIL_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * 按明细状态汇总派生主单状态
     * <p>
     * 判定规则（优先级从高到低）：
     * <ol>
     *   <li>已作废/已入库 为终态，保持不变</li>
     *   <li>运输中 表示尚未确认到货，保持不变（确认到货由业务显式切换为验收中）</li>
     *   <li>明细全部终结且含"待入库" → 验收中（等待点击整单入库）</li>
     *   <li>明细全部终结且含"已入库" → 已入库</li>
     *   <li>明细全部为 退货/取消类（待退货/已退货/已重发/已取消）→ 已结束</li>
     *   <li>存在非终结明细（待初验/待检验）→ 验收中</li>
     * </ol>
     * </p>
     *
     * @param currentStatus 主单当前状态
     * @param detailStatuses 明细状态集合
     * @return 派生后的主单状态
     */
    public static String deriveMainStatus(String currentStatus, List<String> detailStatuses) {
        // 终态保持
        if (MAIN_VOIDED.equals(currentStatus) || MAIN_INBOUND.equals(currentStatus)) {
            return currentStatus;
        }
        // 未确认到货保持运输中（确认到货由 confirmArrival 显式切换）
        if (MAIN_TRANSPORTING.equals(currentStatus)) {
            return MAIN_TRANSPORTING;
        }
        // 无明细时保持现状
        if (detailStatuses == null || detailStatuses.isEmpty()) {
            return currentStatus;
        }
        boolean allTerminal = detailStatuses.stream().allMatch(AcceptanceStatusPolicy::isDetailTerminal);
        if (!allTerminal) {
            return MAIN_INSPECTING;
        }
        // 全部终结：含待入库 → 验收中（等待整单入库）
        if (detailStatuses.contains(DETAIL_PENDING_INBOUND)) {
            return MAIN_INSPECTING;
        }
        // 全部终结：含已入库 → 已入库
        if (detailStatuses.contains(DETAIL_INBOUND)) {
            return MAIN_INBOUND;
        }
        // 全部终结且均为退货/取消类 → 已结束
        if (detailStatuses.stream().allMatch(DETAIL_RETURN_LIKE::contains)) {
            return MAIN_FINISHED;
        }
        return MAIN_INSPECTING;
    }

    /**
     * 汇总明细状态得到"存在退货"派生布尔
     *
     * @param detailStatuses 明细状态集合
     * @return 存在退货返回 true
     */
    public static boolean deriveHasReturn(List<String> detailStatuses) {
        return hasReturn(detailStatuses);
    }

    // endregion
}
