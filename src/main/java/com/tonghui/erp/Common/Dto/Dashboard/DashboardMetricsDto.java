package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页核心指标 DTO（Top1 成本核算 + Top2 生产）
 * <p>
 * 只承载卡片主数值与副信息，明细通过
 * {@code GET /api/dashboard/metrics/details?card=xxx} 单独获取，
 * 避免首页一次性返回全部明细造成响应过大。
 * </p>
 * <p>
 * 金额单位统一为「元」（前端自行换算为万元），数量单位见各字段说明。
 * </p>
 */
@Data
public class DashboardMetricsDto {

    // region 顶层字段
    // ===================================
    // 顶层字段
    // ===================================

    /** 查询区间信息 */
    private RangeDto range;

    /** 成本核算三卡：预估成本 / 实际成本 / 预估产值 */
    private CostMetricsDto cost;

    /** 生产三卡：总计划量 / 总交付量 / 待生产计划量（主数值均为单据数） */
    private ProductionMetricsDto production;

    // endregion

    // region 查询区间
    // ===================================
    // 查询区间
    // ===================================

    /**
     * 查询区间
     */
    @Data
    public static class RangeDto {

        /** 起始月份（yyyy-MM，未传时为 null 表示不限） */
        private String startMonth;

        /** 结束月份（yyyy-MM，未传时为 null 表示不限） */
        private String endMonth;

        /** 区间内月份列表（任一端未传时为空列表，表示全区间） */
        private List<String> months;
    }

    // endregion

    // region 成本核算指标
    // ===================================
    // 成本核算指标
    // ===================================

    /**
     * 成本核算指标集合
     */
    @Data
    public static class CostMetricsDto {

        /** 预估成本 */
        private EstimatedCostCardDto estimatedCost;

        /** 实际成本 */
        private ActualCostCardDto actualCost;

        /** 预估产值 */
        private OutputValueCardDto outputValue;
    }

    /**
     * 预估成本卡片
     * <p>预估成本 = 制剂预计成本价 × 工单批量，按工单配置完成时间归月。</p>
     */
    @Data
    public static class EstimatedCostCardDto {

        /** 预估成本合计（元） */
        private BigDecimal estimatedCost;

        /** 参与核算的批次数（工单数） */
        private Integer batchCount;

        /** 批量合计（件） */
        private BigDecimal batchQty;

        /** 平均预计成本价（元/件）= 预估成本 ÷ 批量合计 */
        private BigDecimal estimatedCostPrice;

        /** 占预估产值比例（%） */
        private BigDecimal ratioOfOutput;

        /** 环比（%），与上一自然月比较 */
        private BigDecimal momRate;

        /** 未维护预计成本价的批次数（按 0 计入） */
        private Integer missingPriceBatchCount;

        /** 未维护预计成本价的批量合计（件） */
        private BigDecimal missingPriceBatchQty;
    }

    /**
     * 实际成本卡片
     * <p>实际成本 = 物料（出库净额）+ 平摊水电气。</p>
     */
    @Data
    public static class ActualCostCardDto {

        /** 实际成本合计（元）= 物料 + 水 + 电 + 气 */
        private BigDecimal actualCost;

        /** 物料成本（元）= 出库金额 − 退库冲减 */
        private BigDecimal materialAmount;

        /** 平摊自来水费（元） */
        private BigDecimal waterAmount;

        /** 平摊电费（元） */
        private BigDecimal electricityAmount;

        /** 平摊燃气费（元） */
        private BigDecimal gasAmount;

        /** 批次数（工单数） */
        private Integer batchCount;

        /** 占预估产值比例（%） */
        private BigDecimal ratioOfOutput;

        /** 偏差（元）= 预估成本 − 实际成本，正数表示预估偏高 */
        private BigDecimal varianceToEstimated;

        /** 偏差率（%）= 偏差 ÷ 预估成本 */
        private BigDecimal varianceRate;
    }

    /**
     * 预估产值卡片
     * <p>预估产值 = 工单总金额，按工单配置完成时间归月。</p>
     */
    @Data
    public static class OutputValueCardDto {

        /** 预估产值合计（元） */
        private BigDecimal outputValue;

        /** 批次数（工单数） */
        private Integer batchCount;

        /** 批量合计（件） */
        private BigDecimal batchQty;

        /** 单位产值（元/件）= 预估产值 ÷ 批量合计 */
        private BigDecimal unitValue;

        /** 环比（%） */
        private BigDecimal momRate;

        /** 预估成本率（%）= 预估成本 ÷ 预估产值 */
        private BigDecimal estimatedCostRatio;

        /** 实际成本率（%）= 实际成本 ÷ 预估产值 */
        private BigDecimal actualCostRatio;
    }

    // endregion

    // region 生产指标
    // ===================================
    // 生产指标
    // ===================================

    /**
     * 生产指标集合（主数值为单据数，数量作为副信息）
     */
    @Data
    public static class ProductionMetricsDto {

        /** 总计划量 */
        private TotalPlanCardDto totalPlan;

        /** 总交付量 */
        private DeliveryCardDto delivery;

        /** 待生产计划量 */
        private PendingCardDto pending;
    }

    /**
     * 总计划量卡片（计划单数为主数值）
     */
    @Data
    public static class TotalPlanCardDto {

        /** 计划单数（张） */
        private Integer planCount;

        /** 计划量合计（件） */
        private BigDecimal planQuantity;

        /** 已交付量合计（件） */
        private BigDecimal deliveredQuantity;

        /** 待生产量合计（件）= Σ max(计划量 − 已交付量, 0) */
        private BigDecimal pendingQuantity;

        /** 平均单张计划量（件/张） */
        private BigDecimal avgQuantity;

        /** 覆盖制剂品种数 */
        private Integer preparationCount;
    }

    /**
     * 总交付量卡片（已交付工单数为主数值）
     */
    @Data
    public static class DeliveryCardDto {

        /** 已交付工单数（张） */
        private Integer workOrderCount;

        /** 批量合计（件） */
        private BigDecimal batchQty;

        /** 计划量交付率（%）= 交付量 ÷ 区间内计划量 */
        private BigDecimal deliveryRate;

        /** 已全部交付的计划单数（张） */
        private Integer fullyDeliveredPlanCount;
    }

    /**
     * 待生产计划量卡片（待生产计划单数为主数值）
     */
    @Data
    public static class PendingCardDto {

        /** 待生产计划单数（张） */
        private Integer planCount;

        /** 计划量合计（件），仅统计待生产的计划单 */
        private BigDecimal planQuantity;

        /** 已交付量合计（件），仅统计待生产的计划单 */
        private BigDecimal deliveredQuantity;

        /** 待生产量合计（件）= Σ max(计划量 − 已交付量, 0) */
        private BigDecimal pendingQuantity;

        /** 占计划量比例（%） */
        private BigDecimal ratioOfPlan;

        /** 涉及制剂品种数 */
        private Integer preparationCount;

        /** 最长滞留天数（天），滞留天数 = 今天 − 计划生产时间 */
        private Long maxOverdueDays;
    }

    // endregion
}
