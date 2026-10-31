package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页图表数据 DTO
 * <p>
 * 覆盖成本核算分析（月度趋势、实际成本构成、原辅包资金三项）
 * 与生产分析（计划 vs 交付、待生产趋势、品种交付排名）。
 * 金额单位统一为「元」，数量单位按分类各自独立。
 * </p>
 */
@Data
public class ChartDataDto {

    // region 顶层字段
    // ===================================
    // 顶层字段
    // ===================================

    /** 月份列表（yyyy-MM，升序；由数据实际存在的月份与查询区间合并得出） */
    private List<String> months;

    /** 月度趋势：预估产值 / 预估成本 / 实际成本 */
    private List<CostTrendItem> costTrend;

    /** 实际成本构成：物料 / 原料 / 辅料 / 包材 / 水 / 电 / 气 / 其他 */
    private List<CostStructureItem> costStructure;

    /** 批次成本偏差排行（按偏差率降序，预计成本为 0 的批次不参与） */
    private List<BatchCostVarianceRankItem> batchCostVarianceRank;

    /** 按品种拆分的实际成本构成（原料 / 辅料 / 包材 / 水电气 / 其他） */
    private List<CostStructureByPreparationItem> costStructureByPreparation;

    /** 原辅包资金三项：入库 / 出库净额 / 当前库存 */
    private MaterialFunds materialFunds;

    /** 生产分析 */
    private ProductionCharts production;

    // endregion

    // region 成本核算图表
    // ===================================
    // 成本核算图表
    // ===================================

    /**
     * 月度成本趋势项
     */
    @Data
    public static class CostTrendItem {

        /** 月份（yyyy-MM） */
        private String month;

        /** 预估产值（元） */
        private BigDecimal outputValue;

        /** 预估成本（元） */
        private BigDecimal estimatedCost;

        /** 实际成本（元） */
        private BigDecimal actualCost;

        /** 成本偏差（元）= 预估成本 − 实际成本，正数表示预估偏高 */
        private BigDecimal costVariance;

        /** 成本偏差率（%）= 成本偏差 ÷ 预估成本，预估成本为 0 时为空 */
        private BigDecimal costVarianceRate;
    }

    /**
     * 月度实际成本构成项
     */
    @Data
    public static class CostStructureItem {

        /** 月份（yyyy-MM） */
        private String month;

        /** 物料成本（元），等于原料 + 辅料 + 包材 */
        private BigDecimal material;

        /** 原料成本（元） */
        private BigDecimal rawMaterial;

        /** 辅料成本（元） */
        private BigDecimal auxiliary;

        /** 包装材料成本（元） */
        private BigDecimal packaging;

        /** 水费（元） */
        private BigDecimal water;

        /** 电费（元） */
        private BigDecimal electricity;

        /** 燃气费（元） */
        private BigDecimal gas;

        /** 其他成本（元） */
        private BigDecimal other;
    }

    /**
     * 批次成本偏差排行项
     * <p>偏差 = 预估成本 − 实际成本；偏差率为正表示成本低于预估（盈利）。</p>
     */
    @Data
    public static class BatchCostVarianceRankItem {

        /** 工单号 */
        private String workOrderCode;

        /** 批号 */
        private String batchNumber;

        /** 制剂编码 */
        private String preparationCode;

        /** 制剂名称 */
        private String preparationName;

        /** 计划单号 */
        private String planNumber;

        /** 批量（件） */
        private BigDecimal batchQty;

        /** 预估成本（元） */
        private BigDecimal estimatedCost;

        /** 实际成本（元） */
        private BigDecimal actualCost;

        /** 成本偏差（元）= 预估成本 − 实际成本 */
        private BigDecimal costVariance;

        /** 成本偏差率（%）= 偏差 ÷ 预估成本 */
        private BigDecimal costVarianceRate;

        /** 批次生产天数（采购下达 → 成品入库） */
        private Long productionDays;
    }

    /**
     * 按品种拆分的实际成本构成项
     */
    @Data
    public static class CostStructureByPreparationItem {

        /** 制剂编码；「未关联制剂」「未分摊水电气」伪品种为空 */
        private String preparationCode;

        /** 制剂名称；伪品种取行类型文案 */
        private String preparationName;

        /** 行类型：正常 / 未关联制剂 / 未分摊水电气 */
        private String rowType;

        /** 原料成本（元） */
        private BigDecimal rawMaterialAmount;

        /** 辅料成本（元） */
        private BigDecimal auxiliaryMaterialAmount;

        /** 包装材料成本（元） */
        private BigDecimal packagingAmount;

        /** 水电气合计（元） */
        private BigDecimal utilityAmount;

        /** 其他成本（元） */
        private BigDecimal otherAmount;

        /** 实际成本合计（元） */
        private BigDecimal totalAmount;

        /** 占区间实际成本合计比例（%） */
        private BigDecimal ratio;
    }

    // endregion

    // region 原辅包资金图表
    // ===================================
    // 原辅包资金图表
    // ===================================

    /**
     * 原辅包资金三项（三项并列，不累加）
     */
    @Data
    public static class MaterialFunds {

        /** 入库（按入库月） */
        private MaterialFundSeries inbound;

        /** 出库净额（出库 − 退库，按出库月） */
        private MaterialFundSeries outbound;

        /** 当前库存（时点快照，忽略月份筛选） */
        private MaterialFundSeries stock;

        /** 库存快照日期（yyyy-MM-dd，仅库存返回） */
        private String snapshotDate;
    }

    /**
     * 资金序列（分类汇总 + 月度明细）
     */
    @Data
    public static class MaterialFundSeries {

        /** 合计金额（元） */
        private BigDecimal totalAmount;

        /** 分类汇总（原料 / 辅料 / 包材） */
        private List<MaterialFundCategory> categories;

        /** 月度分月数据（库存不返回） */
        private List<MaterialFundMonth> months;
    }

    /**
     * 分类汇总
     */
    @Data
    public static class MaterialFundCategory {

        /** 分类：原料 / 辅料 / 包材 */
        private String category;

        /** 金额（元；出库为净额） */
        private BigDecimal amount;

        /** 数量（单位见 unit） */
        private BigDecimal quantity;

        /** 单位：kg / 个 等，按分类各自独立 */
        private String unit;

        /** 占该序列合计金额比例（%） */
        private BigDecimal ratio;

        /** 退库冲减金额（元，仅出库返回） */
        private BigDecimal returnAmount;

        /** 退库冲减数量（仅出库返回） */
        private BigDecimal returnQuantity;
    }

    /**
     * 月度分类数据
     */
    @Data
    public static class MaterialFundMonth {

        /** 月份（yyyy-MM） */
        private String month;

        /** 该月各分类金额与数量 */
        private List<MaterialFundCategory> categories;
    }

    // endregion

    // region 生产图表
    // ===================================
    // 生产图表
    // ===================================

    /**
     * 生产分析图表集合
     */
    @Data
    public static class ProductionCharts {

        /** 计划量 vs 交付量（按月，单位：件） */
        private List<PlanVsDeliveryItem> planVsDelivery;

        /** 待生产计划量（按月，单位：件） */
        private List<PendingMonthItem> pendingByMonth;

        /** 品种交付量 Top 5（单位：件） */
        private List<PreparationRank> topPreparations;

        /** 生产平衡（按批次口径：已全部交付计划 + 待生产计划 = 计划总数） */
        private ProductionBalance balance;
    }

    /**
     * 生产平衡（按批统计）
     * <p>
     * 计划批数 = 已全部交付计划批数 + 待生产计划批数；部分交付的计划归入待生产侧。
     * 比率按批数计算，数量口径为计划量。
     * </p>
     */
    @Data
    public static class ProductionBalance {

        /** 计划批数（区间内计划单数） */
        private Integer planCount;

        /** 计划量合计（件） */
        private BigDecimal planQuantity;

        /** 已全部交付计划批数 */
        private Integer fullyDeliveredPlanCount;

        /** 已全部交付计划量合计（件） */
        private BigDecimal fullyDeliveredPlanQuantity;

        /** 待生产计划批数（含部分交付） */
        private Integer pendingPlanCount;

        /** 待生产计划量合计（件） */
        private BigDecimal pendingPlanQuantity;

        /** 区间内已交付量合计（件） */
        private BigDecimal deliveredQuantity;

        /** 已全部交付批数占比（%） */
        private BigDecimal fullyDeliveredRate;

        /** 待生产批数占比（%） */
        private BigDecimal pendingRate;
    }

    /**
     * 计划量 vs 交付量
     */
    @Data
    public static class PlanVsDeliveryItem {

        /** 月份（yyyy-MM） */
        private String month;

        /** 计划量（件） */
        private BigDecimal planQuantity;

        /** 交付量（件） */
        private BigDecimal deliveryQuantity;
    }

    /**
     * 月度待生产量
     */
    @Data
    public static class PendingMonthItem {

        /** 月份（yyyy-MM） */
        private String month;

        /** 待生产量（件） */
        private BigDecimal pendingQuantity;
    }

    /**
     * 品种交付量排名
     */
    @Data
    public static class PreparationRank {

        /** 制剂编码 */
        private String preparationCode;

        /** 制剂名称 */
        private String preparationName;

        /** 交付量（件） */
        private BigDecimal deliveryQuantity;
    }

    // endregion
}
