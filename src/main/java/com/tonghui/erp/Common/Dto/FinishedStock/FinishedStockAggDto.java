package com.tonghui.erp.Common.Dto.FinishedStock;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 成品库存聚合数据 DTO
 * <p>
 * 用于接收 StockMapper 按品种编码汇总后的成品库存数据，
 * 包含总库存、近效期库存与已过期库存，供预警服务计算可用库存
 * </p>
 */
@Data
public class FinishedStockAggDto {

    // region 品种信息
    // ===================================
    // 品种信息
    // ===================================

    /**
     * 制剂编码（库存物品编码）
     */
    private String preparationCode;

    /**
     * 制剂名称（库存物品名称）
     */
    private String preparationName;

    /**
     * 计量单位
     */
    private String unitName;

    // endregion

    // region 库存数量
    // ===================================
    // 库存数量
    // ===================================

    /**
     * 成品总库存数量（含待检、不合格等全部状态）
     */
    private BigDecimal totalStock;

    /**
     * 合格成品库存数量（可用库存的计算基数）
     */
    private BigDecimal qualifiedStock;

    /**
     * 合格成品中的近效期库存数量（有效期 <= 今天 + 近效期天数，含已过期）
     */
    private BigDecimal nearExpiryStock;

    /**
     * 已过期库存数量（有效期 < 今天）
     */
    private BigDecimal expiredStock;

    // endregion
}
