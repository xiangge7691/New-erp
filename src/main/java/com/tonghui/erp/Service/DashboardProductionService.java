package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Dashboard.CardDetailDto;
import com.tonghui.erp.Common.Dto.Dashboard.ChartDataDto;
import com.tonghui.erp.Common.Dto.Dashboard.DashboardDetailRowDto;
import com.tonghui.erp.Common.Dto.Dashboard.DashboardMetricsDto;

/**
 * 首页生产统计服务
 * <p>
 * 负责 Top2 生产三张卡片（总计划量 / 总交付量 / 待生产计划量）、
 * 三份卡片明细与生产分析图表。
 * </p>
 * <p>
 * 口径：三张卡片主数值均为「单据数」，数量作为副信息，单位统一为「件」；
 * 待生产量 = 计划量 − 已交付量（差额大于 0 的计划计入待生产）。
 * </p>
 */
public interface DashboardProductionService {

    // region 卡片指标
    // ===================================
    // 卡片指标
    // ===================================

    /**
     * 构建生产三张卡片
     *
     * @param startMonth 起始月份（yyyy-MM，可空）
     * @param endMonth   结束月份（yyyy-MM，可空）
     * @return 生产指标
     */
    DashboardMetricsDto.ProductionMetricsDto getProductionMetrics(String startMonth, String endMonth);

    // endregion

    // region 卡片明细
    // ===================================
    // 卡片明细
    // ===================================

    /**
     * 总计划量明细（生产计划单粒度）
     *
     * 示例请求：
     * GET /api/dashboard/metrics/details?card=totalPlan&startMonth=2026-05&endMonth=2026-10
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 汇总 + 全量明细
     */
    CardDetailDto<DashboardMetricsDto.TotalPlanCardDto, DashboardDetailRowDto>
    getTotalPlanDetails(String startMonth, String endMonth);

    /**
     * 总交付量明细（工单粒度）
     *
     * 示例请求：
     * GET /api/dashboard/metrics/details?card=delivery&startMonth=2026-05&endMonth=2026-10
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 汇总 + 全量明细
     */
    CardDetailDto<DashboardMetricsDto.DeliveryCardDto, DashboardDetailRowDto>
    getDeliveryDetails(String startMonth, String endMonth);

    /**
     * 待生产明细（待生产计划单粒度）
     *
     * 示例请求：
     * GET /api/dashboard/metrics/details?card=pending&startMonth=2026-05&endMonth=2026-10
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 汇总 + 全量明细
     */
    CardDetailDto<DashboardMetricsDto.PendingCardDto, DashboardDetailRowDto>
    getPendingDetails(String startMonth, String endMonth);

    // endregion

    // region 图表数据
    // ===================================
    // 图表数据
    // ===================================

    /**
     * 生产分析图表（计划 vs 交付 / 月度待生产 / 品种交付 Top5）
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 生产分析图表
     */
    ChartDataDto.ProductionCharts getProductionCharts(String startMonth, String endMonth);

    // endregion
}
