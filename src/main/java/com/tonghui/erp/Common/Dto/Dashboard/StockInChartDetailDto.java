package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;
import java.util.List;

/**
 * 月度入库金额图表按月份+类别分组的明细DTO
 * <p>
 * charts 接口 monthlyStockInAmount.details 中的每一项，
 * 表示某个月份下某个类别（原料/辅料/包材）的入库明细
 * </p>
 */
@Data
public class StockInChartDetailDto {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /**
     * 月份（如"9月"）
     */
    private String month;

    /**
     * 库存类别（原料/辅料/包材）
     */
    private String category;

    /**
     * 该类别下的入库明细记录
     */
    private List<StockInChartRecordDto> records;

    // endregion
}
