package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 调拨单详情明细项DTO
 * <p>
 * 展示每条物料批次调拨的调出/调入仓库、前后库存、调拨数量及金额
 * </p>
 */
@Data
public class TransferOrderDetailItemDto {

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 分类（原料/辅料/包材/成品）
     */
    private String category;

    /**
     * 批号
     */
    private String batchNo;

    /**
     * 调出仓库
     */
    private String srcWarehouse;

    /**
     * 调入仓库
     */
    private String dstWarehouse;

    /**
     * 调出前库存
     */
    private BigDecimal srcStock;

    /**
     * 调入前库存
     */
    private BigDecimal dstStock;

    /**
     * 调拨数量
     */
    private BigDecimal transferQuantity;

    /**
     * 单价（元）
     */
    private BigDecimal unitPrice;

    /**
     * 金额（元）
     */
    private BigDecimal amount;

    /**
     * 计量单位
     */
    private String unit;
}