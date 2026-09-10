package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存批次明细数据传输对象
 * <p>
 * 用于库存分组查询中每个物料下的批次明细展示
 * </p>
 */
@Data
public class StockBatchDto {

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
     * 仓库名称（冗余展示，来自生产单位表）
     */
    private String warehouseName;

    /**
     * 库存状态：合格/待检/不合格
     */
    private String stockStatus;

    /**
     * 库存数量
     */
    private BigDecimal quantity;

    /**
     * 生产日期
     */
    private LocalDate productionDate;

    /**
     * 有效期至
     */
    private LocalDate expiryDate;

    /**
     * 单价
     */
    private BigDecimal unitPrice;
}
