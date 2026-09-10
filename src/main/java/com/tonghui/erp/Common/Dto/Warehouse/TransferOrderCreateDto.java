package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增调拨单请求DTO
 * <p>
 * 调拨单创建请求体：指定调出/调入仓库及调拨物料批次明细（库存标识+调拨数量）
 * </p>
 */
@Data
public class TransferOrderCreateDto {

    /**
     * 调出仓库名称
     */
    private String fromWarehouse;

    /**
     * 调入仓库名称（不能与调出仓库相同）
     */
    private String toWarehouse;

    /**
     * 备注
     */
    private String remark;

    /**
     * 调拨物料批次明细列表（库存标识+调拨数量）
     */
    private List<TransferItemRequest> items;
}