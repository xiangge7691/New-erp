package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 批次明细数据传输对象
 * <p>
 * 用于库存分组查询中每个批次+制剂下的明细展示，
 * 包含物料信息、仓库信息、库存状态及来源入库单号
 * </p>
 */
@Data
public class StockBatchDetailDto {

    /**
     * 库存ID
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
     * 分类名称（原料/辅料/包材/成品）
     */
    private String categoryName;

    /**
     * 计量单位
     */
    private String unitName;

    /**
     * 仓库名称
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
}
