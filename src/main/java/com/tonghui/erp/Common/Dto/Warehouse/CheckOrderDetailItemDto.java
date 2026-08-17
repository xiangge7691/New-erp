package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 盘点单详情明细项DTO
 * <p>
 * 展示每个盘点批次的系统库存、实盘数量、差异与盘点结果
 * </p>
 */
@Data
public class CheckOrderDetailItemDto {

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
     * 物料状态（合格/待检/不合格）
     */
    private String status;

    /**
     * 系统库存
     */
    private BigDecimal systemStock;

    /**
     * 实盘数量
     */
    private BigDecimal actualStock;

    /**
     * 差异（实盘-系统）
     */
    private BigDecimal difference;

    /**
     * 盘点结果（盘盈/盘亏/盘平）
     */
    private String result;

    /**
     * 计量单位
     */
    private String unit;
}