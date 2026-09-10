package com.tonghui.erp.Common.Dto.Stock;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 批量出库明细项数据传输对象
 * <p>
 * 批量出库确认请求中的单个出库明细，携带库存批次ID用于扣减定位
 * </p>
 */
@Data
public class BatchOutboundItemDto {

    /**
     * 库存批次ID（必填，用于扣减定位）
     */
    private Long stockId;

    /**
     * 物料编码
     */
    private String itemCode;

    /**
     * 物料名称
     */
    private String itemName;

    /**
     * 分类名称（原料/辅料/包材/成品）
     */
    private String categoryName;

    /**
     * 单位名称
     */
    private String unitName;

    /**
     * 批次号
     */
    private String batchNumber;

    /**
     * 出库数量
     */
    private BigDecimal quantity;

    /**
     * 单价
     */
    private BigDecimal unitPrice;
}
