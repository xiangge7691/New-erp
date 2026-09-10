package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.util.List;

/**
 * 批量出库确认请求数据传输对象
 * <p>
 * 一次事务内创建出库单（草稿）并确认生效（扣减库存+写流水）
 * </p>
 */
@Data
public class BatchOutboundRequest {

    /**
     * 出库类型：生产领料出库/销售出库/报损出库
     */
    private String outType;

    /**
     * 关联单号（生产计划编号/销售单号等）
     */
    private String relatedOrder;

    /**
     * 出库仓库（生产单位ID）
     */
    private Long prodUnitId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 出库明细列表
     */
    private List<BatchOutboundItemDto> items;
}
