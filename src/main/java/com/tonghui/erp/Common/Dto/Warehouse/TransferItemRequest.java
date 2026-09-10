package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 调拨请求项DTO
 * <p>
 * 指定要调拨的库存批次（库存标识：编码_仓库_批号）及调拨数量
 * </p>
 */
@Data
public class TransferItemRequest {

    /**
     * 调出库存标识（格式：物料编码_仓库名_批号）
     */
    private String inventoryKey;

    /**
     * 调拨数量（必须大于0且不超过调出库存）
     */
    private BigDecimal transferQuantity;
}