package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 产值图表单条工单记录DTO
 * <p>
 * 对应一个有配置完成时间的生产工单
 * </p>
 */
@Data
public class RevenueChartRecordDto {

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
     * 配置完成时间
     */
    private String configCompleteTime;

    // endregion
}
