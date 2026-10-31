package com.tonghui.erp.Common.Dto.FinishedStock;

import lombok.Data;

/**
 * 成品库存预警汇总 DTO
 * <p>
 * 用于成品库存预警页面顶部统计卡片与系统配置展示，
 * 返回品种总数、预警数量、各预警级别数量及当前统计口径
 * </p>
 */
@Data
public class FinishedStockWarningSummaryDto {

    // region 统计数量
    // ===================================
    // 统计数量
    // ===================================

    /**
     * 品种总数
     */
    private long totalProducts;

    /**
     * 触发预警的品种数
     */
    private long warningCount;

    /**
     * 可支撑时间不足 1 个月的品种数
     */
    private long dangerCount;

    /**
     * 库存正常的品种数（有出库且未触发预警）
     */
    private long normalCount;

    /**
     * 统计窗口内无出库的品种数
     */
    private long noDemandCount;

    /**
     * 可用库存为 0 的品种数
     */
    private long noStockCount;

    // endregion

    // region 统计口径
    // ===================================
    // 统计口径
    // ===================================

    /**
     * 统计月数（前 N 个完整自然月）
     */
    private int avgMonths;

    /**
     * 预警阈值（月）
     */
    private int warningMonths;

    /**
     * 近效期天数
     */
    private int nearExpiryDays;

    /**
     * 统计窗口起始月份（格式：yyyy-MM）
     */
    private String avgStartMonth;

    /**
     * 统计窗口结束月份（格式：yyyy-MM）
     */
    private String avgEndMonth;

    /**
     * 是否剔除近效期库存
     */
    private boolean excludeNearExpiry;

    // endregion
}
