package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Dashboard.CardDetailDto;
import com.tonghui.erp.Common.Dto.Dashboard.ChartDataDto;
import com.tonghui.erp.Common.Dto.Dashboard.DashboardDetailRowDto;
import com.tonghui.erp.Common.Dto.Dashboard.DashboardMetricsDto;

import java.util.List;

/**
 * 首页成本核算统计服务
 * <p>
 * 负责 Top1 成本核算三张卡片（预估成本 / 实际成本 / 预估产值）、
 * 三份卡片明细，以及月度成本趋势与实际成本构成图表。
 * </p>
 * <p>
 * 统一口径：
 * <ul>
 *   <li>预估产值 = 工单 total_amount，按 config_complete_time 归月</li>
 *   <li>预估成本 = 制剂预计成本价 × 工单批量，按 config_complete_time 归月</li>
 *   <li>物料成本 = 出库明细金额 − 退库冲减，先归生产计划再按批量分摊到工单</li>
 *   <li>水电气 = 能耗记录当月金额，按当月已交付工单数均摊</li>
 * </ul>
 * </p>
 */
public interface DashboardCostService {

    // region 卡片指标
    // ===================================
    // 卡片指标
    // ===================================

    /**
     * 构建成本核算三张卡片
     *
     * @param startMonth 起始月份（yyyy-MM，可空）
     * @param endMonth   结束月份（yyyy-MM，可空）
     * @return 成本核算指标
     */
    DashboardMetricsDto.CostMetricsDto getCostMetrics(String startMonth, String endMonth);

    // endregion

    // region 卡片明细
    // ===================================
    // 卡片明细
    // ===================================

    /**
     * 预估成本明细（工单粒度）
     *
     * 示例请求：
     * GET /api/dashboard/metrics/details?card=estimatedCost&startMonth=2026-05&endMonth=2026-10
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 汇总 + 全量明细
     */
    CardDetailDto<DashboardMetricsDto.EstimatedCostCardDto, DashboardDetailRowDto>
    getEstimatedCostDetails(String startMonth, String endMonth);

    /**
     * 实际成本明细（按月份 + 工单合并，含未关联制剂 / 未分摊水电气行）
     *
     * 示例请求：
     * GET /api/dashboard/metrics/details?card=actualCost&startMonth=2026-05&endMonth=2026-10
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 汇总 + 全量明细
     */
    CardDetailDto<DashboardMetricsDto.ActualCostCardDto, DashboardDetailRowDto>
    getActualCostDetails(String startMonth, String endMonth);

    /**
     * 预估产值明细（工单粒度）
     *
     * 示例请求：
     * GET /api/dashboard/metrics/details?card=outputValue&startMonth=2026-05&endMonth=2026-10
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 汇总 + 全量明细
     */
    CardDetailDto<DashboardMetricsDto.OutputValueCardDto, DashboardDetailRowDto>
    getOutputValueDetails(String startMonth, String endMonth);

    // endregion

    // region 图表数据
    // ===================================
    // 图表数据
    // ===================================

    /**
     * 月度成本趋势（产值 / 预估成本 / 实际成本）
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 月度趋势列表（升序）
     */
    List<ChartDataDto.CostTrendItem> getCostTrend(String startMonth, String endMonth);

    /**
     * 月度实际成本构成（物料 / 水 / 电 / 气）
     *
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @return 月度构成列表（升序）
     */
    List<ChartDataDto.CostStructureItem> getCostStructure(String startMonth, String endMonth);

    // endregion
}
