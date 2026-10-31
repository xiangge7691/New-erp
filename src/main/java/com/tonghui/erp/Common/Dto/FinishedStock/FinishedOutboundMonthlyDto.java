package com.tonghui.erp.Common.Dto.FinishedStock;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 成品出库按月聚合数据 DTO
 * <p>
 * 用于接收 SalesOrderMapper 按品种编码 + 自然月汇总后的已出库数量，
 * 供成品库存预警服务计算月平均出库量
 * </p>
 */
@Data
public class FinishedOutboundMonthlyDto {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /**
     * 制剂编码
     */
    private String preparationCode;

    /**
     * 制剂名称
     */
    private String preparationName;

    /**
     * 统计月份（格式：yyyy-MM）
     */
    private String month;

    /**
     * 该月已出库数量
     */
    private BigDecimal quantity;

    // endregion
}
