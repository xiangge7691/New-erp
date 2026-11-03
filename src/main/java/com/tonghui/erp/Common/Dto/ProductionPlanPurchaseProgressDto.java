package com.tonghui.erp.Common.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产计划采购流程详情传输对象
 * <p>
 * 返回生产计划下所有采购链路、六个流程节点、节点间耗时以及五个子页面的关联单据。
 * 列表接口只返回采购摘要，本对象用于点击后的详情抽屉/子页面。
 * </p>
 */
@Data
public class ProductionPlanPurchaseProgressDto {

    // region 生产计划信息
    // ===================================
    // 生产计划信息
    // ===================================

    /** 生产计划ID */
    private Integer productionPlanId;

    /** 生产计划编号 */
    private String planNumber;

    /** 生产计划名称 */
    private String planName;

    /** 生产计划当前状态 */
    private String currentStatus;

    /** 计划生产时间 */
    private LocalDateTime planProductionTime;

    /** 需交付时间 */
    private LocalDateTime deliveryTime;

    // endregion

    // region 汇总与逾期
    // ===================================
    // 汇总与逾期
    // ===================================

    /** 采购进度摘要 */
    private ProductionPlanPurchaseSummaryDto purchaseSummary;

    /** 生产计划逾期信息 */
    private ProductionPlanOverdueDto overdue;

    // endregion

    // region 链路与关联单据
    // ===================================
    // 链路与关联单据
    // ===================================

    /** 采购链路列表，一个采购订单一条链路 */
    private List<PurchaseChainDto> chains;

    /** 关联单据分组 */
    private ProductionPlanRelatedDocumentsDto relatedDocuments;

    // endregion
}
