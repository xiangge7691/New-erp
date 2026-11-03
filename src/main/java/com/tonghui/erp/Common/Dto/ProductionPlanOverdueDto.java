package com.tonghui.erp.Common.Dto;

import lombok.Data;

/**
 * 生产计划逾期信息传输对象
 * <p>
 * 用于列表和详情返回计划生产时间、需交付时间的逾期标记及逾期时长，
 * 前端依据该对象对时间列标红，无需自行比较时间。
 * </p>
 * <p>
 * 逾期口径（按自然日计算）：
 * <ul>
 *   <li>计划生产时间：实际生产开始时间与计划生产时间比较；尚未开始生产时与当前时间比较</li>
 *   <li>需交付时间：成品实际出库时间与需交付时间比较；尚未出库时与当前时间比较</li>
 *   <li>已完成/已归档计划保留历史逾期结果；作废计划不计算逾期</li>
 * </ul>
 * </p>
 */
@Data
public class ProductionPlanOverdueDto {

    // region 计划生产时间逾期
    // ===================================
    // 计划生产时间逾期
    // ===================================

    /** 计划生产时间是否逾期 */
    private Boolean planProductionTime;

    /** 计划生产时间逾期自然日数，未逾期为 0 */
    private Long planProductionOverdueDays;

    /**
     * 计划生产时间逾期分钟数，未逾期为 0
     *
     * @deprecated 逾期已改为按自然日计算，请改用 {@link #planProductionOverdueDays}
     */
    @Deprecated
    private Long planProductionOverdueMinutes;

    /** 计划生产时间逾期文案，如“实际晚了3天”，未逾期为 null */
    private String planProductionTimeText;

    // endregion

    // region 需交付时间逾期
    // ===================================
    // 需交付时间逾期
    // ===================================

    /** 需交付时间是否逾期 */
    private Boolean deliveryTime;

    /** 需交付时间逾期自然日数，未逾期为 0 */
    private Long deliveryOverdueDays;

    /**
     * 需交付时间逾期分钟数，未逾期为 0
     *
     * @deprecated 逾期已改为按自然日计算，请改用 {@link #deliveryOverdueDays}
     */
    @Deprecated
    private Long deliveryOverdueMinutes;

    /** 需交付时间逾期文案，如“实际晚了3天”，未逾期为 null */
    private String deliveryTimeText;

    // endregion

    // region 汇总
    // ===================================
    // 汇总
    // ===================================

    /** 逾期级别：NONE/PLAN/DELIVERY/BOTH */
    private String level;

    /** 逾期展示文案（计划生产时间与需交付时间的合并文案） */
    private String text;

    // endregion
}
