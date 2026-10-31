package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 成本与生产明细树 DTO（月份 → 制剂 → 批次）
 * <p>
 * 对应首页「明细下钻」区块：一级为月份（金额前端按万元展示），
 * 二级为制剂，三级为批次（工单）。
 * </p>
 * <p>
 * 口径与 {@code /api/dashboard/metrics} 六张卡片保持一致：
 * <ul>
 *   <li>预估产值、预估成本、交付量按 {@code config_complete_time} 归月</li>
 *   <li>实际成本按出库 / 能耗发生的月份归月，可能与该工单的交付月份不同</li>
 *   <li>因此同一工单可能在两个月份各出现一行：交付月（产值 / 预估成本）与成本月（物料 / 水电气）</li>
 * </ul>
 * 金额单位统一为「元」，数量单位「件」。
 * </p>
 */
@Data
public class DashboardTreeDto {

    // region 顶层字段
    // ===================================
    // 顶层字段
    // ===================================

    /** 查询区间回显 */
    private String startMonth;

    /** 查询区间回显 */
    private String endMonth;

    /** 区间合计与计数（对应区块标题右侧「N 个月 / N 个制剂 / N 批」） */
    private Summary summary;

    /** 月份节点列表（升序，仅包含有数据的月份） */
    private List<Month> months;

    // endregion

    // region 区间合计
    // ===================================
    // 区间合计
    // ===================================

    /**
     * 区间合计与计数
     */
    @Data
    public static class Summary {

        /** 有数据的月份数 */
        private int monthCount;

        /** 涉及制剂数（去重，不含「未关联制剂」「未分摊水电气」伪节点） */
        private int preparationCount;

        /** 涉及批次数（去重工单数） */
        private int batchCount;

        /** 预估产值合计（元） */
        private BigDecimal outputValue;

        /** 预估成本合计（元） */
        private BigDecimal estimatedCost;

        /** 实际成本合计（元） */
        private BigDecimal actualCost;

        /** 成本偏差（元）= 预估成本 − 实际成本 */
        private BigDecimal costVariance;

        /** 成本偏差率（%）= 偏差 ÷ 预估成本 */
        private BigDecimal costVarianceRate;

        /** 交付量合计（件） */
        private BigDecimal deliveryQuantity;
    }

    // endregion

    // region 月份节点
    // ===================================
    // 月份节点
    // ===================================

    /**
     * 月份节点（一级）
     */
    @Data
    public static class Month {

        /** 月份（yyyy-MM） */
        private String month;

        /** 预估产值合计（元；前端展示为万元） */
        private BigDecimal outputValue;

        /** 预估成本合计（元；前端展示为万元） */
        private BigDecimal estimatedCost;

        /** 实际成本合计（元；前端展示为万元） */
        private BigDecimal actualCost;

        /** 成本偏差（元）= 预估成本 − 实际成本 */
        private BigDecimal costVariance;

        /** 成本偏差率（%）= 偏差 ÷ 预估成本 */
        private BigDecimal costVarianceRate;

        /** 交付量合计（件） */
        private BigDecimal deliveryQuantity;

        /** 制剂节点列表（按预估产值降序） */
        private List<Preparation> preparations;
    }

    // endregion

    // region 制剂节点
    // ===================================
    // 制剂节点
    // ===================================

    /**
     * 制剂节点（二级）
     */
    @Data
    public static class Preparation {

        /** 制剂编码；「未关联制剂」「未分摊水电气」伪节点为 null */
        private String preparationCode;

        /** 制剂名称；伪节点取下方的 rowType 文案 */
        private String preparationName;

        /** 行类型：正常 / 未关联制剂 / 未分摊水电气 */
        private String rowType;

        /** 预估产值合计（元） */
        private BigDecimal outputValue;

        /** 预估成本合计（元） */
        private BigDecimal estimatedCost;

        /** 实际成本合计（元） */
        private BigDecimal actualCost;

        /** 成本偏差（元）= 预估成本 − 实际成本 */
        private BigDecimal costVariance;

        /** 成本偏差率（%）= 偏差 ÷ 预估成本 */
        private BigDecimal costVarianceRate;

        /** 交付量合计（件） */
        private BigDecimal deliveryQuantity;

        /** 批次节点列表 */
        private List<Batch> batches;
    }

    // endregion

    // region 批次节点
    // ===================================
    // 批次节点
    // ===================================

    /**
     * 批次节点（三级，粒度为工单）
     */
    @Data
    public static class Batch {

        /** 工单号（伪节点为 null） */
        private String workOrderCode;

        /** 批号 */
        private String batchNumber;

        /** 计划单号 */
        private String planNumber;

        /** 工单状态 / 出库状态 */
        private String status;

        /** 时间（交付月为配置完成时间，成本月为出库 / 能耗时间） */
        private String time;

        /** 行类型：正常 / 未关联制剂 / 未分摊水电气 */
        private String rowType;

        /** 批量（件） */
        private BigDecimal batchQty;

        /** 预估产值（元）；成本月行为 null */
        private BigDecimal outputValue;

        /** 预估成本（元）；成本月行为 null */
        private BigDecimal estimatedCost;

        /** 物料成本（元） */
        private BigDecimal materialAmount;

        /** 原料成本（元） */
        private BigDecimal rawMaterialAmount;

        /** 辅料成本（元） */
        private BigDecimal auxiliaryMaterialAmount;

        /** 包装材料成本（元） */
        private BigDecimal packagingAmount;

        /** 水电气合计（元）= 水 + 电 + 气 */
        private BigDecimal utilityAmount;

        /** 其他成本（元） */
        private BigDecimal otherAmount;

        /** 实际成本（元）= 原辅包 + 水电气 + 其他 */
        private BigDecimal actualCost;

        /** 单位预计成本（元/件，快照价） */
        private BigDecimal unitEstimatedCost;

        /** 单位实际成本（元/件）= 实际成本 ÷ 批量 */
        private BigDecimal unitActualCost;

        /** 单位成本偏差（元/件）= 单位预计成本 − 单位实际成本 */
        private BigDecimal unitCostVariance;

        /** 单位成本偏差率（%）= 单位成本偏差 ÷ 单位预计成本 */
        private BigDecimal unitCostVarianceRate;

        /** 采购需求下达时间 */
        private String purchaseDate;

        /** 成品入库时间 */
        private String inboundDate;

        /** 批次生产天数（采购下达 → 成品入库） */
        private Long productionDays;
    }

    // endregion
}
