package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 卡片明细行（成本核算与生产通用）
 * <p>
 * 六种卡片（预估成本 / 实际成本 / 预估产值 / 总计划量 / 总交付量 / 待生产）
 * 共用同一行结构，前端按卡片类型取用对应字段，未使用字段为 null。
 * </p>
 */
@Data
public class DashboardDetailRowDto {

    // region 通用字段
    // ===================================
    // 通用字段
    // ===================================

    /** 月份（yyyy-MM；库存为时点值时不返回） */
    private String month;

    /** 制剂编码 */
    private String preparationCode;

    /** 制剂名称 */
    private String preparationName;

    /** 工单号 */
    private String workOrderCode;

    /** 批号 */
    private String batchNumber;

    /** 计划单号 */
    private String planNumber;

    /** 单据状态（工单状态 / 计划状态） */
    private String status;

    /** 时间（交付=配置完成时间；计划=计划生产时间） */
    private String time;

    /** 行类型：正常 / 未关联制剂 / 未分摊水电气 */
    private String rowType;

    // endregion

    // region 成本字段
    // ===================================
    // 成本字段
    // ===================================

    /** 批量（件） */
    private BigDecimal batchQty;

    /** 预计成本价（元/件） */
    private BigDecimal estimatedCostPrice;

    /** 预估成本（元） */
    private BigDecimal estimatedCost;

    /** 预估产值（元） */
    private BigDecimal outputValue;

    /** 单位产值（元/件） */
    private BigDecimal unitValue;

    /** 物料成本（元） */
    private BigDecimal materialAmount;

    /** 原料成本（元） */
    private BigDecimal rawMaterialAmount;

    /** 辅料成本（元） */
    private BigDecimal auxiliaryMaterialAmount;

    /** 包装材料成本（元） */
    private BigDecimal packagingAmount;

    /** 水费（元） */
    private BigDecimal waterAmount;

    /** 电费（元） */
    private BigDecimal electricityAmount;

    /** 燃气费（元） */
    private BigDecimal gasAmount;

    /** 其他成本（元） */
    private BigDecimal otherAmount;

    /** 实际成本（元）= 原辅包 + 水 + 电 + 气 + 其他 */
    private BigDecimal actualCost;

    /** 单位预计成本（元/件，快照价） */
    private BigDecimal unitEstimatedCost;

    /** 单位实际成本（元/件）= 实际成本 ÷ 批量 */
    private BigDecimal unitActualCost;

    /** 单位成本偏差（元/件）= 单位预计成本 − 单位实际成本 */
    private BigDecimal unitCostVariance;

    /** 单位成本偏差率（%）= 单位成本偏差 ÷ 单位预计成本 */
    private BigDecimal unitCostVarianceRate;

    // endregion

    // region 批次周期字段
    // ===================================
    // 批次周期字段
    // ===================================

    /** 采购需求下达时间（同一计划多张采购单取最早创建时间） */
    private String purchaseDate;

    /** 成品入库时间（工单入库时间，缺失时取成品入库单日期） */
    private String inboundDate;

    /** 批次生产天数（采购下达 → 成品入库），任一端缺失为 null */
    private Long productionDays;

    // region 生产字段
    // ===================================
    // 生产字段
    // ===================================

    /** 计划量（件） */
    private BigDecimal planQuantity;

    /** 已交付量（件） */
    private BigDecimal deliveredQuantity;

    /** 待生产量（件）= 计划量 − 已交付量 */
    private BigDecimal pendingQuantity;

    /** 滞留天数（天），仅待生产明细返回 */
    private Long overdueDays;

    // endregion
}
