package com.tonghui.erp.Common.Dto.Preparation;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 制剂处方明细（含库存汇总数量）DTO
 * <p>
 * 在处方明细基础上补充该原料在库存中的汇总数量（按物料编码汇总所有批次、生产单位），
 * 用于制剂生产/领料时查看原料可用库存
 * </p>
 */
@Data
public class PreparationFormulaStockDto {

    // region 制剂信息
    // ===================================
    // 制剂信息
    // ===================================

    /**
     * 制剂ID
     */
    private Long preparationId;

    /**
     * 制剂编码
     */
    private String preparationCode;

    /**
     * 制剂品名
     */
    private String preparationName;

    // endregion

    // region 处方明细信息
    // ===================================
    // 处方明细信息
    // ===================================

    /**
     * 处方明细唯一标识
     */
    private Long formulaId;

    /**
     * 原料ID
     */
    private Long materialId;

    /**
     * 原料编号
     */
    private String materialCode;

    /**
     * 原料名称
     */
    private String materialName;

    /**
     * 原料分类（原料/辅料/包材）
     */
    private String materialCategory;

    /**
     * 处方量
     */
    private BigDecimal dosage;

    /**
     * 单位ID
     */
    private Long unitId;

    /**
     * 单位名称
     */
    private String unitName;

    // endregion

    // region 库存信息
    // ===================================
    // 库存信息
    // ===================================

    /**
     * 库存汇总数量（按物料编码汇总所有批次、生产单位的库存，无库存为0）
     */
    private BigDecimal stockQuantity;

    // endregion
}
