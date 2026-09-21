package com.tonghui.erp.Common.Dto;

import com.tonghui.erp.Data.Entity.StockOutDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 成品出库台账新增请求DTO
 * <p>
 * 包含台账基本信息和出库明细列表，创建台账时自动联动创建草稿出库单
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SalesOrderCreateDto {

    // region 台账基本信息
    // ===================================
    // 台账基本信息
    // ===================================

    /**
     * 成品出库日期
     */
    private LocalDateTime salesOrderDate;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 制剂名称
     */
    private String preparationName;

    /**
     * 制剂编码
     */
    private String preparationCode;

    /**
     * 批号
     */
    private String batchNumber;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 备注
     */
    private String remark;

    // endregion

    // region 出库明细列表（创建台账时联动创建草稿出库单）
    // ===================================
    // 出库明细列表
    // ===================================

    /**
     * 出库明细列表（传入后自动创建草稿出库单）
     */
    private List<StockOutDetail> stockOutDetails;

    // endregion
}
