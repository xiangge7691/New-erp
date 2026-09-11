package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存分组查询明细数据传输对象（第二级 - 扁平明细）
 * <p>
 * 每行代表一条独立的库存记录，包含批号、制剂名称、仓库、状态、数量、来源等信息。
 * 按批号排序，同一物料下的所有记录扁平展示
 * </p>
 */
@Data
public class StockBatchDetailDto {

    /**
     * 库存ID
     */
    private Long stockId;

    /**
     * 批号
     */
    private String batchNumber;

    /**
     * 制剂名称
     */
    private String preparationName;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 仓库ID（生产单位ID）
     */
    private Long prodUnitId;

    /**
     * 库存状态：合格/待检/不合格
     */
    private String stockStatus;

    /**
     * 库存数量
     */
    private BigDecimal quantity;

    /**
     * 来源入库单号
     */
    private String relatedOrderCode;

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

    /**
     * 金额（quantity × unitPrice，后端计算）
     */
    private BigDecimal amount;
}
