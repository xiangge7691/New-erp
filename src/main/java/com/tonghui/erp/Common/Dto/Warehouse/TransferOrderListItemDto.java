package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调拨单列表项DTO
 * <p>
 * 调拨记录列表展示用，包含调拨单主表关键信息
 * </p>
 */
@Data
public class TransferOrderListItemDto {

    /**
     * 调拨单ID
     */
    private Long id;

    /**
     * 调拨单号
     */
    private String transferNo;

    /**
     * 调出仓库
     */
    private String fromWarehouse;

    /**
     * 调入仓库
     */
    private String toWarehouse;

    /**
     * 物料种数（批次数）
     */
    private Integer materialCount;

    /**
     * 调拨总量
     */
    private BigDecimal totalQuantity;

    /**
     * 调拨总价（元）
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