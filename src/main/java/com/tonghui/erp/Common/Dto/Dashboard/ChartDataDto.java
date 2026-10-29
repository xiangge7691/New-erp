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

    /** 实际成本构成：物料 / 水 / 电 / 气 */
    private List<CostStructureItem> costStructure;

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
    }

    /**
     * 月度实际成本构成项
     */
    @Data
    public static class CostStructureItem {

        /** 月份（yyyy-MM） */
        private String month;

        /** 物料成本（元） */
        private BigDecimal material;

        /** 水费（元） */
        private BigDecimal water;

        /** 电费（元） */
        private BigDecimal electricity;

        /** 燃气费（元） */
        private BigDecimal gas;
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
