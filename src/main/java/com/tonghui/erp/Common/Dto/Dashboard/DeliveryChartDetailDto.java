package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;
import java.util.List;

/**
 * 交付数量图表按剂型大类分组的明细DTO
 * <p>
 * charts 接口 deliveryByDosageForm.details 中的每一项，
 * 表示某个月份下某个剂型大类的明细记录
 * </p>
 */
@Data
public class DeliveryChartDetailDto {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /** 月份（如"8月"） */
    private String month;

    /** 剂型大类（如"散剂"、"合剂"） */
    private String dosageCategory;

    /** 该剂型下的生产计划明细 */
    private List<DeliveryChartRecordDto> records;

    // endregion
}
