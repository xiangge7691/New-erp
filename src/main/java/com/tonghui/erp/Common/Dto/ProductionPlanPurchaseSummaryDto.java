package com.tonghui.erp.Common.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 生产计划采购进度摘要传输对象
 * <p>
 * 用于生产计划列表展示关联采购的整体进度。一个生产计划可关联多条采购计划/采购订单，
 * 摘要按“最慢链路”原则计算当前阶段，避免部分订单已完成导致整体进度被高估。
 * </p>
 */
@Data
public class ProductionPlanPurchaseSummaryDto {

    // region 采购单据统计
    // ===================================
    // 采购单据统计
    // ===================================

    /** 是否存在采购计划 */
    private Boolean hasPurchasePlan;

    /** 采购计划数量 */
    private Integer purchasePlanCount;

    /** 采购订单数量 */
    private Integer purchaseOrderCount;

    /** 已入库/已归档订单数量 */
    private Integer completedOrderCount;

    /** 已结束订单数量（如全部退货） */
    private Integer closedOrderCount;

    /** 异常链路数量（采购计划驳回、订单作废、验收单作废等） */
    private Integer abnormalCount;

    // endregion

    // region 进度汇总
    // ===================================
    // 进度汇总
    // ===================================

    /** 当前阶段编码：APPLY/APPROVE/ORDER/ARRIVE/ACCEPT/INBOUND/ABNORMAL */
    private String currentStageCode;

    /** 当前阶段名称 */
    private String currentStageName;

    /** 当前阶段状态：CURRENT/DONE/ABNORMAL */
    private String currentStageState;

    /** 最慢链路展示状态：待审批/待下单/运输中/待验收/待入库/已完成/异常/未采购 */
    private String displayStatus;

    /** 最慢链路ID，便于列表直接定位到对应选项卡 */
    private String bottleneckChainId;

    /** 整体进度百分比（取最慢链路） */
    private Integer progressPercent;

    /** 各阶段链路数量分布，key 为节点编码 */
    private Map<String, Integer> stageDistribution;

    /** 是否存在“采购计划已审批但未生成采购订单”的数据不一致 */
    private Boolean dataInconsistent;

    /** 采购进度展示文案 */
    private String summaryText;

    /** 最近一次采购节点时间（用于列表展示） */
    private LocalDateTime latestNodeTime;

    // endregion
}
