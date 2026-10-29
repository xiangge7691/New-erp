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

    /** 水费（元） */
    private BigDecimal waterAmount;

    /** 电费（元） */
    private BigDecimal electricityAmount;

    /** 燃气费（元） */
    private BigDecimal gasAmount;

    /** 实际成本（元）= 物料 + 水 + 电 + 气 */
    private BigDecimal actualCost;

    // endregion

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
