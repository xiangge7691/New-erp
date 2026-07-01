package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 图表数据DTO
 */
@Data
public class ChartDataDto {
    /**
     * 交付数量按剂型（月度）
     * key: 月份, value: {剂型: 数量}
     */
    private List<Map<String, Object>> deliveryByDosageForm;

    /**
     * 预估产值（月度）
     * key: 月份, value: {剂型: 金额}
     */
    private List<Map<String, Object>> revenueByMonth;

    /**
     * 库存资金占用
     * key: 类别, value: 金额
     */
    private Map<String, Double> inventoryFundOccupation;
}
