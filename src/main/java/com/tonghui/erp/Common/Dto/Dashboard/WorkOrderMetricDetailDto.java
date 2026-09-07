package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 预估产值/总交付量指标对应的工单明细DTO
 * <p>
 * 用于 metrics 接口中"预估产值"和"总交付量"两个指标的 details 字段
 * </p>
 */
@Data
public class WorkOrderMetricDetailDto {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /**
     * 工单ID
     */
    private Long workOrderId;

    /**
     * 工单编号
     */
    private String workOrderCode;

    /**
     * 制剂编码
     */
    private String preparationCode;

    /**
     * 制剂名称
     */
    private String preparationName;

    /**
     * 批量
     */
    private BigDecimal batchQty;

    /**
     * 预估产值（元）
     */
    private BigDecimal totalAmount;

    /**
     * 当前状态
     */
    private String currentStatus;

    /**
     * 配置日期
     */
    private String configDate;

    /**
     * 配置完成时间
     */
    private String configCompleteTime;

    /**
     * 生产完成时间
     */
    private String productionCompleteTime;

    // endregion
}
