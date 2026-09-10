package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退库单列表项DTO
 * <p>
 * 退库记录列表展示用，包含退库单主表关键信息
 * </p>
 */
@Data
public class ReturnOrderListItemDto {

    /**
     * 退库单ID
     */
    private Long id;

    /**
     * 退库单号
     */
    private String returnNo;

    /**
     * 关联出库单号
     */
    private String outOrderNo;

    /**
     * 生产计划编号
     */
    private String productionPlanNo;

    /**
     * 物料种数
     */
    private Integer materialCount;

    /**
     * 退库总量
     */
    private BigDecimal totalQuantity;

    /**
     * 退库总价（元）
     */
    private BigDecimal totalAmount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作时间
     */
    private LocalDateTime createdAt;
}