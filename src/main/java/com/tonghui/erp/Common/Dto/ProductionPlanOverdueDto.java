package com.tonghui.erp.Common.Dto;

import lombok.Data;

/**
 * 生产计划逾期信息传输对象
 * <p>
 * 用于列表和详情返回计划生产时间、需交付时间的逾期标记及逾期时长，
 * 前端依据该对象对时间列标红，无需自行比较时间。
 * </p>
 */
@Data
public class ProductionPlanOverdueDto {

    // region 逾期标记
    // ===================================
    // 逾期标记
    // ===================================

    /** 计划生产时间是否逾期 */
    private Boolean planProductionTime;

    /** 计划生产时间逾期分钟数，未逾期为 0 */
    private Long planProductionOverdueMinutes;

    /** 需交付时间是否逾期 */
    private Boolean deliveryTime;

    /** 需交付时间逾期分钟数，未逾期为 0 */
    private Long deliveryOverdueMinutes;

    /** 逾期级别：NONE/PLAN/DELIVERY/BOTH */
    private String level;

    /** 逾期展示文案 */
    private String text;

    // endregion
}
