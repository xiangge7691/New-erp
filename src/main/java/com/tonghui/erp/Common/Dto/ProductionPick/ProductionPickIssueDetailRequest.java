package com.tonghui.erp.Common.Dto.ProductionPick;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 生产领料发料明细请求数据传输对象
 * <p>
 * 每条明细对应一张领料明细行，actualQty 为实发数量；
 * stockId 可选，未指定时由后端按物料编码 FIFO 自动分配库存批次。
 * </p>
 */
@Data
public class ProductionPickIssueDetailRequest {

    /**
     * 生产领料明细ID
     */
    private Long detailId;

    /**
     * 实发数量
     */
    private BigDecimal actualQty;

    /**
     * 指定库存批次ID（可选，未指定则按 FIFO 自动分配）
     */
    private Long stockId;
}
