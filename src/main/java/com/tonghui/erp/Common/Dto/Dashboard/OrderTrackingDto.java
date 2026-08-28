package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

/**
 * 订单跟踪DTO
 */
@Data
public class OrderTrackingDto {

    // region 订单基本信息
    // ===================================
    // 订单基本信息
    // ===================================

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

    // endregion

    // region 日期信息
    // ===================================
    // 日期信息
    // ===================================

    /**
     * 下单日期
     */
    private String orderDate;

    /**
     * 生产日期（取最晚工单的 configCompleteTime）
     */
    private String productionDate;

    /**
     * 检验日期（取最晚工单的 inspectionEnd）
     */
    private String inspectionDate;

    /**
     * 出库日期（取最晚工单的 outboundTime）
     */
    private String outboundDate;

    /**
     * 归档日期（取最晚工单的 archiveTime）
     */
    private String archiveDate;

    // endregion
}
