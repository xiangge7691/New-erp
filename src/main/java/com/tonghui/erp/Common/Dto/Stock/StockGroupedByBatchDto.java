package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存分组查询数据传输对象
 * <p>
 * 按 批次号 + 制剂名称 分组聚合库存，
 * 用于库存查询页面按"批次+制剂分组"的展示模式
 * </p>
 */
@Data
public class StockGroupedByBatchDto {

    /**
     * 批次号
     */
    private String batchNumber;

    /**
     * 制剂名称
     */
    private String preparationName;

    /**
     * 总库存数量（该批次+制剂下所有记录之和）
     */
    private BigDecimal totalQuantity;

    /**
     * 明细列表（同批次+制剂下的不同库存记录）
     */
    private List<StockBatchDetailDto> details;
}
