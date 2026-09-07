package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;
import java.util.List;

/**
 * 预估产值图表按剂型大类分组的明细DTO
 * <p>
 * charts 接口 revenueByMonth.details 中的每一项，
 * 表示某个月份下某个剂型大类的工单明细
 * </p>
 */
@Data
public class RevenueChartDetailDto {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /**
     * 月份（如"8月"）
     */
    private String month;

    /**
     * 剂型大类
     */
    private String dosageCategory;

    /**
     * 该剂型下的工单明细
     */
    private List<RevenueChartRecordDto> records;

    // endregion
}
