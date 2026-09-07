package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 图表数据DTO（含明细）
 * <p>
 * 每个图表包含 summary（月度聚合数据，用于图表渲染）和
 * details（按月份+剂型/类别两级分组的明细记录列表，用于展开查看）
 * </p>
 */
@Data
public class ChartDataDto {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /**
     * 交付数量按剂型（月度）
     * <p>summary: 每月各剂型的交付数量聚合；details: 按月份+剂型分组的生产计划明细</p>
     */
    private DeliveryChartData deliveryByDosageForm;

    /**
     * 预估产值（月度）
     * <p>summary: 每月各剂型的产值聚合；details: 按月份+剂型分组的工单明细</p>
     */
    private RevenueChartData revenueByMonth;

    /**
     * 库存资金占用
     * <p>summary: 按类别的总价值；details: 按类别分组的库存物品明细</p>
     */
    private InventoryChartData inventoryFundOccupation;

    // endregion

    // region 嵌套数据结构
    // ===================================
    // 嵌套数据结构
    // ===================================

    /**
     * 交付数量图表数据
     */
    @Data
    public static class DeliveryChartData {
        /** 月度聚合数据（前端柱状图/折线图渲染） */
        private List<Map<String, Object>> summary;
        /** 按月份+剂型分组的明细记录 */
        private List<DeliveryChartDetailDto> details;
    }

    /**
     * 预估产值图表数据
     */
    @Data
    public static class RevenueChartData {
        /** 月度聚合数据（前端柱状图/折线图渲染） */
        private List<Map<String, Object>> summary;
        /** 按月份+剂型分组的明细记录 */
        private List<RevenueChartDetailDto> details;
    }

    /**
     * 库存资金占用图表数据
     */
    @Data
    public static class InventoryChartData {
        /** 按类别的总价值（前端饼图/环形图渲染） */
        private Map<String, Double> summary;
        /** 按类别分组的库存物品明细 */
        private List<InventoryChartDetailDto> details;
    }

    // endregion
}
