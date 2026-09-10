package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增退库单请求DTO
 * <p>
 * 退库单创建请求体：指定关联出库单号及退库物料明细（库存标识+退库数量）
 * </p>
 */
@Data
public class ReturnOrderCreateDto {

    /**
     * 关联出库单号
     */
    private String outOrderNo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 退库物料明细列表（库存标识+退库数量）
     */
    private List<ReturnItemRequest> items;
}