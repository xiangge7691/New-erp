package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 可用库存批次数据传输对象
 * <p>
 * 用于批量出库按处方匹配时，展示某物料可出库的合格库存批次（FIFO排序）
 * </p>
 */
@Data
public class AvailableBatchDto {

    /**
     * 库存批次ID
     */
    private Long stockId;

    /**
     * 物料编码
     */
    private String itemCode;

    /**
     * 物料名称
     */
    private String itemName;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 单位名称
     */
    private String unitName;

    /**
     * 批次号
     */
    private String batchNumber;

    /**
     * 仓库（生产单位ID）
     */
    private Long prodUnitId;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 可用库存数量
     */
    private BigDecimal quantity;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 金额（应出数量×该批次单价）
     */
    private BigDecimal amount;

    /**
     * 库存状态
     */
    private String stockStatus;

    /**
     * 关联生产计划编号
     */
    private String planNumber;

    /**
     * 来源入库单号
     */
    private String relatedOrderCode;
}
