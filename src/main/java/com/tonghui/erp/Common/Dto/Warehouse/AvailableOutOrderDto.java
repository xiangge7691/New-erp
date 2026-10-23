package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 可退库出库单DTO
 * <p>
 * 新增退库时展示可退库的出库单（生产领料出库且仍有可退额度）
 * </p>
 */
@Data
public class AvailableOutOrderDto {

    /**
     * 出库单号
     */
    private String outOrderNo;

    /**
     * 生产计划编号
     */
    private String productionPlanNo;

    /**
     * 生产计划名称（制剂名称）
     */
    private String productionPlanName;

    /**
     * 可退物料分类（对有可退额度的明细分类去重后按字典序拼接，以","分隔）
     * <p>示例：原料,包材；无分类时返回空字符串</p>
     */
    private String materialCategories;

    /**
     * 可退物料种数（有可退额度的明细条数）
     */
    private Integer materialCount;

    /**
     * 可退总量
     */
    private BigDecimal totalAvailableQuantity;
}