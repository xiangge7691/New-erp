package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量出库处方明细数据传输对象
 * <p>
 * 按生产计划关联制剂处方生成，包含每个物料的应出数量（处方量×生产倍数）及可用库存批次
 * </p>
 */
@Data
public class PlanDetailItemDto {

    /**
     * 序号（返回时按处方顺序填充，从1开始）
     */
    private Integer index;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 物料分类（原料/辅料/包材）
     */
    private String materialCategory;

    /**
     * 单位名称
     */
    private String unitName;

    /**
     * 标准处方量
     */
    private BigDecimal dosage;

    /**
     * 应出数量（处方量×生产倍数）
     */
    private BigDecimal requiredQty;

    /**
     * 可用库存批次列表（合格批次，FIFO排序）
     */
    private List<AvailableBatchDto> availableBatches;
}
