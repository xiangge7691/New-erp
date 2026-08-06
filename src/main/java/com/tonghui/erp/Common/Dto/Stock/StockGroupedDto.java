package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存分组查询数据传输对象
 * <p>
 * 按物料编码分组聚合库存，包含该物料下所有批次的明细列表，
 * 用于库存查询页面按"物料分组 + 批次展开"的展示模式
 * </p>
 */
@Data
public class StockGroupedDto {

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
     * 总库存数量（所有批次之和）
     */
    private BigDecimal totalQuantity;

    /**
     * 批次数
     */
    private int batchCount;

    /**
     * 批次明细列表
     */
    private List<StockBatchDto> batches;
}
