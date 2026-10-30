package com.tonghui.erp.Common.Dto.Production;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 按制剂统计的平均生产时间
 * <p>
 * 用于生产计划/生产任务列表下方的平均生产时间展示，按制剂维度汇总
 * </p>
 */
@Data
public class PreparationDurationStatDto {

    /**
     * 制剂ID
     */
    private Long preparationId;

    /**
     * 制剂编码
     */
    private String preparationCode;

    /**
     * 制剂名称
     */
    private String preparationName;

    /**
     * 平均生产时间（天，保留两位小数）
     */
    private BigDecimal avgDays;

    /**
     * 参与统计的有效样本数
     */
    private long sampleCount;
}
