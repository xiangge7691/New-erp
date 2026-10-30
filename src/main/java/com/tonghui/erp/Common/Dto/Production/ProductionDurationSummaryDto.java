package com.tonghui.erp.Common.Dto.Production;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 生产用时汇总统计
 * <p>
 * 按当前查询条件统计总体平均生产时间，并按制剂维度拆分，
 * 供生产计划/生产任务表格下方展示
 * </p>
 */
@Data
public class ProductionDurationSummaryDto {

    /**
     * 总体平均生产时间（天，保留两位小数）
     */
    private BigDecimal avgDays;

    /**
     * 参与统计的有效样本数
     */
    private long sampleCount;

    /**
     * 按制剂拆分的平均生产时间
     */
    private List<PreparationDurationStatDto> byPreparation = new ArrayList<>();
}
