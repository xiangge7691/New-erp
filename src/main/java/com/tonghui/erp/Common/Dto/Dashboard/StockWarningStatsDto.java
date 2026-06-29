package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

/**
 * 库存预警统计DTO
 */
@Data
public class StockWarningStatsDto {
    /**
     * 库存不足数量
     */
    private long lowStock;

    /**
     * 即将过期数量
     */
    private long expiringSoon;

    /**
     * 已过期数量
     */
    private long expired;
}
