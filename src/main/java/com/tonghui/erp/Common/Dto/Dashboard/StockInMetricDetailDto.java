package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

/**
 * 总采购额指标对应的入库单明细DTO
 * <p>
 * 用于 metrics 接口中"总采购额"指标的 details 字段
 * </p>
 */
@Data
public class StockInMetricDetailDto {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /** 入库单ID */
    private Long inId;

    /** 入库单号 */
    private String inCode;

    /** 入库类型 */
    private String inType;

    /** 总金额 */
    private Object totalAmount;

    /** 入库状态 */
    private String inStatus;

    /** 入库日期 */
    private String inDate;

    // endregion
}
