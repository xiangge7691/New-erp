package com.tonghui.erp.Common.Dto.FinishedStock;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 月度出库量 DTO
 * <p>
 * 用于前端展示品种在统计窗口内每个自然月的出库数量及趋势
 * </p>
 */
@Data
public class MonthlyOutboundDto {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /**
     * 统计月份（格式：yyyy-MM）
     */
    private String month;

    /**
     * 该月已出库数量（无出库时补 0）
     */
    private BigDecimal quantity;

    // endregion
}
