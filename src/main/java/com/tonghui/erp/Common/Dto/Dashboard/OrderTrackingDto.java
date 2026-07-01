package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

/**
 * 订单跟踪DTO
 */
@Data
public class OrderTrackingDto {
    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单名称（制剂名称）
     */
    private String orderName;

    /**
     * 数量
     */
    private String quantity;

    /**
     * 批号
     */
    private String batchNo;

    /**
     * 所属医疗机构
     */
    private String hospital;

    /**
     * 当前状态
     */
    private String currentStatus;

    /**
     * 下单日期
     */
    private String orderDate;

    /**
     * 采购日期
     */
    private String purchaseDate;

    /**
     * 生产日期
     */
    private String productionDate;

    /**
     * 检验日期
     */
    private String inspectionDate;

    /**
     * 出库日期
     */
    private String outboundDate;

    /**
     * 归档日期
     */
    private String archiveDate;
}
