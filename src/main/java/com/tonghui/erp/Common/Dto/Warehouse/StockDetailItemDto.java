package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 盘点用库存明细DTO
 * <p>
 * 获取仓库库存明细（盘点用），展示库存标识、系统库存等信息供盘点录入实盘数量
 * </p>
 */
@Data
public class StockDetailItemDto {

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
     * 系统库存数量
     */
    private BigDecimal systemStock;

    /**
     * 计量单位
     */
    private String unit;
}