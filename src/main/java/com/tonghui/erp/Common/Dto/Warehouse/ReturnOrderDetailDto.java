package com.tonghui.erp.Common.Dto.Warehouse;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 退库单详情DTO
 * <p>
 * 退库单主表信息 + 退库明细列表，用于退库单详情展示
 * </p>
 */
@Data
public class ReturnOrderDetailDto {

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
     * 生产计划名称
     */
    private String productionPlanName;

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

    /**
     * 退库明细列表
     */
    private List<ReturnOrderDetailItemDto> items;
}