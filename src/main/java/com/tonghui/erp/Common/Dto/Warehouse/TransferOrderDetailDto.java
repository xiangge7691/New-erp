package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 调拨单详情DTO
 * <p>
 * 调拨单主表信息 + 调拨明细列表，用于调拨单详情展示
 * </p>
 */
@Data
public class TransferOrderDetailDto {

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

    /**
     * 调拨明细列表
     */
    private List<TransferOrderDetailItemDto> items;
}