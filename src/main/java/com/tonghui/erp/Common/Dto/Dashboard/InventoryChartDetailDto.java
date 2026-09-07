package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;
import java.util.List;

/**
 * 库存资金占用图表按类别分组的明细DTO
 * <p>
 * charts 接口 inventoryFundOccupation.details 中的每一项，
 * 表示某个库存类别下的物品明细
 * </p>
 */
@Data
public class InventoryChartDetailDto {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /** 库存类别（如"原料"、"包材"） */
    private String category;

    /** 该类别下的库存物品明细 */
    private List<InventoryChartRecordDto> records;

    // endregion
}
