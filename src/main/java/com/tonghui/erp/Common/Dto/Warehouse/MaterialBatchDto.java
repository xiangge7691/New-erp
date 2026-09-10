package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 物料批次详情DTO
 * <p>
 * 新增调拨时展示某物料在某仓库的批次库存信息，用于选择调拨批次
 * </p>
 */
@Data
public class MaterialBatchDto {

    /**
     * 库存标识（格式：物料编码_仓库名_批号）
     */
    private String inventoryKey;

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
     * 库存数量
     */
    private BigDecimal stock;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 单价（元）
     */
    private BigDecimal unitPrice;
}