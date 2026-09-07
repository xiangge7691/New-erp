package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 交付图表单条记录DTO
 * <p>
 * 对应一个已完成或生产中的生产计划
 * </p>
 */
@Data
public class DeliveryChartRecordDto {

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
     * 计划生产时间
     */
    private String planProductionTime;

    /**
     * 当前状态（待生产/生产中/已完成）
     */
    private String currentStatus;

    // endregion
}
