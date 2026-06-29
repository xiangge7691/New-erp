package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

/**
 * 生产计划统计DTO
 */
@Data
public class ProductionStatsDto {
    /**
     * 计划总数
     */
    private long totalPlans;

    /**
     * 进行中数量
     */
    private long inProgress;

    /**
     * 已完成数量
     */
    private long completed;

    /**
     * 待处理数量
     */
    private long pending;
}
