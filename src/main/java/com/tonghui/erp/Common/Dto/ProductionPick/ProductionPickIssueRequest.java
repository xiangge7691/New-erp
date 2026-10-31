package com.tonghui.erp.Common.Dto.ProductionPick;

import lombok.Data;

import java.util.List;

/**
 * 生产领料发料请求数据传输对象
 * <p>
 * 发料时记录审核人、发料人及每条明细的实发数量/库存批次，
 * 后端据此生成“生产领料出库”草稿出库单。
 * </p>
 */
@Data
public class ProductionPickIssueRequest {

    /**
     * 审核人
     */
    private String reviewer;

    /**
     * 发料人
     */
    private String issuer;

    /**
     * 发料明细列表
     */
    private List<ProductionPickIssueDetailRequest> details;
}
