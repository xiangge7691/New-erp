package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 退库请求项DTO
 * <p>
 * 指定要退库的出库明细批次（库存标识：编码_仓库_批号）及本次退库数量
 * </p>
 */
@Data
public class ReturnItemRequest {

    /**
     * 库存标识（格式：物料编码_仓库名_批号）
     */
    private String inventoryKey;

    /**
     * 本次退库数量（必须大于0且不超过可退数量）
     */
    private BigDecimal returnQuantity;
}