package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 盘点请求项DTO
 * <p>
 * 指定要盘点的库存批次（库存标识：编码_仓库_批号）及实盘数量
 * </p>
 */
@Data
public class CheckItemRequest {

    /**
     * 库存标识（格式：物料编码_仓库名_批号）
     */
    private String inventoryKey;

    /**
     * 实盘数量（不能为负数）
     */
    private BigDecimal actualStock;
}