package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

/**
 * 库存图表单条库存记录DTO
 * <p>
 * 对应一条库存记录，包含物料信息、数量、单价及总价值
 * </p>
 */
@Data
public class InventoryChartRecordDto {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /** 库存ID */
    private Long stockId;

    /** 物料编码 */
    private String itemCode;

    /** 物料名称 */
    private String itemName;

    /** 类别名称 */
    private String categoryName;

    /** 单位 */
    private String unitName;

    /** 数量 */
    private Object quantity;

    /** 单价 */
    private Object unitPrice;

    /** 总价值（数量 × 单价） */
    private Object totalValue;

    /** 批号 */
    private String batchNumber;

    // endregion
}
