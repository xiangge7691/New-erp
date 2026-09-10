package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存流水详细数据传输对象
 * <p>
 * 在库存交易记录基础上扩展物品信息、仓库信息、关联单据号、业务单号及操作人姓名，
 * 用于库存查询页面查看细化流水时展示完整的追溯信息
 * </p>
 */
@Data
public class StockTransactionDetailDto {

    // region 基本信息
    // ===================================
    // 基本信息
    // ===================================

    /**
     * 流水ID
     */
    private Long transactionId;

    /**
     * 交易类型（入库/出库/调整）
     */
    private String transactionType;

    /**
     * 交易时间
     */
    private LocalDateTime transactionDate;

    // endregion

    // region 物品信息
    // ===================================
    // 物品信息
    // ===================================

    /**
     * 物品编码
     */
    private String itemCode;

    /**
     * 物品名称
     */
    private String itemName;

    /**
     * 批次号
     */
    private String batchNumber;

    /**
     * 分类名称（原料/辅料/包材/成品）
     */
    private String categoryName;

    /**
     * 计量单位
     */
    private String unitName;

    // endregion

    // region 仓库信息
    // ===================================
    // 仓库信息
    // ===================================

    /**
     * 仓库ID
     */
    private Long prodUnitId;

    /**
     * 仓库名称
     */
    private String warehouseName;

    // endregion

    // region 数量信息
    // ===================================
    // 数量信息
    // ===================================

    /**
     * 交易前数量
     */
    private BigDecimal quantityBefore;

    /**
     * 变动数量（正数增加，负数减少）
     */
    private BigDecimal quantityChange;

    /**
     * 交易后数量
     */
    private BigDecimal quantityAfter;

    // endregion

    // region 关联单据
    // ===================================
    // 关联单据
    // ===================================

    /**
     * 关联单据号（入库单号/出库单号/调拨单号/盘点单号/退库单号）
     */
    private String relatedDocCode;

    /**
     * 关联单据类型（入库/出库/调拨/盘点/退库）
     */
    private String relatedDocType;

    /**
     * 业务单号（采购单/生产计划/生产任务/销售单等）
     */
    private String relatedOrderCode;

    /**
     * 业务单类型
     */
    private String relatedOrderType;

    // endregion

    // region 操作人
    // ===================================
    // 操作人
    // ===================================

    /**
     * 操作人姓名
     */
    private String createdByName;

    // endregion
}
