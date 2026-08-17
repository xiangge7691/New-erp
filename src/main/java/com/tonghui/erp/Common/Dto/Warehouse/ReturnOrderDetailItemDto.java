package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 退库单详情明细项DTO
 * <p>
 * 展示每条出库明细的出库数量、本次退库数量、单价与金额
 * </p>
 */
@Data
public class ReturnOrderDetailItemDto {

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
     * 仓库名称
     */
    private String warehouse;

    /**
     * 出库数量
     */
    private BigDecimal outQuantity;

    /**
     * 本次退库数量
     */
    private BigDecimal returnQuantity;

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