package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

/**
 * 核心指标DTO（含明细）
 * <p>
 * 每个指标字段由汇总值（summary）和明细记录列表（details）组成，
 * 前端可同时展示"数字卡片"和"展开明细列表"
 * </p>
 */
@Data
public class DashboardMetricsDto {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /**
     * 预估产值：汇总值（万元）+ 工单明细
     */
    private MetricsItem<Double, WorkOrderMetricDetailDto> estimatedOutputValue;

    /**
     * 总订单量：汇总值（单）+ 生产计划明细
     */
    private MetricsItem<Long, ProductionPlanMetricDetailDto> totalOrders;

    /**
     * 总交付量：汇总值（单）+ 已交付工单明细
     */
    private MetricsItem<Long, WorkOrderMetricDetailDto> totalDeliveries;

    /**
     * 总采购额：汇总值（万元）+ 入库单明细
     */
    private MetricsItem<Double, StockInMetricDetailDto> totalPurchaseAmount;

    /**
     * 待生产数量：汇总值（批）+ 待生产计划明细
     */
    private MetricsItem<Long, ProductionPlanMetricDetailDto> pendingProduction;

    // endregion
}
