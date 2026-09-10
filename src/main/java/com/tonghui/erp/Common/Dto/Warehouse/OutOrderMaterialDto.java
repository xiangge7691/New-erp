package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 出库单物料明细DTO（含可退数量）
 * <p>
 * 新增退库时展示出库单中各物料的出库数量、已退数量与可退数量
 * </p>
 */
@Data
public class OutOrderMaterialDto {

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
     * 计量单位
     */
    private String unit;

    /**
     * 出库数量
     */
    private BigDecimal outQuantity;

    /**
     * 已退数量
     */
    private BigDecimal returnedQuantity;

    /**
     * 可退数量（出库数量 - 已退数量）
     */
    private BigDecimal availableQuantity;

    /**
     * 单价（元）
     */
    private BigDecimal unitPrice;
}