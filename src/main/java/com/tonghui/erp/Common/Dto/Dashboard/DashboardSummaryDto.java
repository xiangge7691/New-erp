package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

/**
 * 首页仪表盘汇总数据DTO
 * 包含生产统计、库存预警、审批统计、最近活动
 */
@Data
public class DashboardSummaryDto {
    /**
     * 生产计划统计
     */
    private ProductionStatsDto productionStats;

    /**
     * 库存预警统计
     */
    private StockWarningStatsDto stockWarnings;

    /**
     * 审批统计
     */
    private ApprovalStatsDto approvalStats;
}
