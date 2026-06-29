package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存有效期预警 DTO（基于 FIFO 先进先出计算）
 */
@Data
public class ExpiryWarningDTO {
    
    /**
     * 入库明细ID（作为预警记录的唯一标识）
     */
    private Long inDetailId;
    
    /**
     * 关联的库存ID（stock表中的记录）
     */
    private Long stockId;
    
    /**
     * 物品类型: material/preparation
     */
    private String itemType;
    
    /**
     * 物品ID
     */
    private Long itemId;
    
    /**
     * 物品编码
     */
    private String itemCode;
    
    /**
     * 物品名称
     */
    private String itemName;
    
    /**
     * 分类名称
     */
    private String categoryName;
    
    /**
     * 计量单位
     */
    private String unitName;
    
    /**
     * 批次号
     */
    private String batchNumber;
    
    /**
     * 入库数量（原始入库明细中的数量）
     */
    private BigDecimal inQuantity;
    
    /**
     * 已出库数量（通过 FIFO 计算）
     */
    private BigDecimal outQuantity;
    
    /**
     * 实际剩余数量（入库数量 - 已出库数量）
     */
    private BigDecimal remainingQuantity;
    
    /**
     * 有效期至（来自入库明细）
     */
    private LocalDate expiryDate;
    
    /**
     * 生产日期（来自入库明细）
     */
    private LocalDate productionDate;
    
    /**
     * 剩余天数
     */
    private Integer remainingDays;
    
    /**
     * 预警级别: urgent(≤7天) / warning(≤30天) / info(≤90天)
     */
    private String warningLevel;
    
    /**
     * 生产单位ID
     */
    private Long prodUnitId;
    
    /**
     * 库位/货架号
     */
    private String storageLocation;
    
    // ========== 入库追溯信息 ==========
    
    /**
     * 入库单ID
     */
    private Long inId;
    
    /**
     * 入库单号
     */
    private String inCode;
    
    /**
     * 入库日期
     */
    private LocalDate inDate;
    
    /**
     * 入库单状态
     */
    private String inStatus;
}
