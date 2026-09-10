package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 月度入库金额图表单条入库明细记录DTO
 * <p>
 * 对应一条入库明细（stock_in_detail），关联入库单获取日期信息
 * </p>
 */
@Data
public class StockInChartRecordDto {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /**
     * 入库明细ID
     */
    private Long inDetailId;

    /**
     * 入库单号
     */
    private String inCode;

    /**
     * 物料编码
     */
    private String itemCode;

    /**
     * 物料名称
     */
    private String itemName;

    /**
     * 库存类别（原料/辅料/包材）
     */
    private String categoryName;

    /**
     * 入库数量
     */
    private BigDecimal quantity;

    /**
     * 单价（元）
     */
    private BigDecimal unitPrice;

    /**
     * 金额（元）
     */
    private BigDecimal amount;

    /**
     * 入库日期
     */
    private String inDate;

    // endregion
}
