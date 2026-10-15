package com.tonghui.erp.Common.Dto.Maintenance;

import lombok.Data;

/**
 * 待养护统计数据传输对象
 * <p>
 * 首页展示「今日待养护数量、逾期数量」，数量按待检查的库存明细数统计，
 * 点击进入对应的待养护列表
 * </p>
 */
@Data
public class MaintenancePendingSummaryDto {

    /**
     * 已逾期数量（已超过下次养护日期，尚未完成检查）
     */
    private long overdueCount;

    /**
     * 今日到期数量（下次养护日期为今天）
     */
    private long todayCount;

    /**
     * 即将到期数量（距下次养护日期在提前提醒天数内）
     */
    private long soonCount;

    /**
     * 提前提醒天数（系统配置，默认3天）
     */
    private int remindDays;
}
