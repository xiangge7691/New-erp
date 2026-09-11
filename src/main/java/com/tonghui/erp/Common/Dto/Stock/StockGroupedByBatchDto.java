package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存分组查询数据传输对象（第一级 - 物料分组）
 * <p>
 * 按物料编码(itemCode)分组聚合库存，
 * 用于库存查询页面按"物料维度"的展示模式。
 * 每个物料组内包含扁平的明细列表，按批号排序
 * </p>
 */
@Data
public class StockGroupedByBatchDto {

    /**
     * 物料编码
     */
    private String itemCode;

    /**
     * 物料名称
     */
    private String itemName;

    /**
     * 分类名称（原料/辅料/包材/成品）
     */
    private String categoryName;

    /**
     * 计量单位
     */
    private String unitName;

    /**
     * 批次数（distinct batchNumber 数量）
     */
    private int batchCount;

    /**
     * 条目数（明细列表行数）
     */
    private int entryCount;

    /**
     * 总库存数量（该物料所有记录的数量之和）
     */
    private BigDecimal totalQuantity;

    /**
     * 总金额（所有记录 quantity × unitPrice 之和）
     */
    private BigDecimal totalValue;

    /**
     * 明细列表（扁平结构，每行 = 1条库存记录，按批号排序）
     */
    private List<StockBatchDetailDto> details;
}
