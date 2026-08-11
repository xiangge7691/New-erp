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
}
