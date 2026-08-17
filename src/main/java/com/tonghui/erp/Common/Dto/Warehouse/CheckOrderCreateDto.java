package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.util.List;

/**
 * 提交盘点请求DTO
 * <p>
 * 盘点单创建请求体：指定盘点仓库及盘点明细（库存标识+实盘数量）
 * </p>
 */
@Data
public class CheckOrderCreateDto {

    /**
     * 盘点仓库名称
     */
    private String warehouse;

    /**
     * 盘点明细列表（库存标识+实盘数量）
     */
    private List<CheckItemRequest> items;
}