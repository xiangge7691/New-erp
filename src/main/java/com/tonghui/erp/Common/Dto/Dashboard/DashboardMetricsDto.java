package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

/**
 * 核心指标DTO
 */
@Data
public class DashboardMetricsDto {
    /**
     * 预估产值（万元）
     */
    private Double estimatedOutputValue;

    /**
     * 总订单量（单）
     */
    private Long totalOrders;

    /**
     * 总交付量（单）
     */
    private Long totalDeliveries;

    /**
     * 总采购额（万元）
     */
    private Double totalPurchaseAmount;

    /**
     * 待生产数量（批）
     */
    private Long pendingProduction;
}
