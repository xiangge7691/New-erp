package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 总订单量/待生产数量指标对应的生产计划明细DTO
 * <p>
 * 用于 metrics 接口中"总订单量"和"待生产数量"两个指标的 details 字段
 * </p>
 */
@Data
public class ProductionPlanMetricDetailDto {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /**
     * 计划ID
     */
    private Integer planId;

    /**
     * 计划编号
     */
    private String planNumber;

    /**
     * 制剂编码
     */
    private String preparationCode;

    /**
     * 制剂名称
     */
    private String preparationName;

    /**
     * 计划数量
     */
    private BigDecimal planQuantity;

    /**
     * 当前状态
     */
    private String currentStatus;

    /**
     * 计划生产时间
     */
    private String planProductionTime;

    /**
     * 创建时间
     */
    private String createdTime;

    // endregion
}
