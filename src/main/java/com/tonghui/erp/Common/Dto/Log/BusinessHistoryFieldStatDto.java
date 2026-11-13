package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;

/**
 * 业务变更历史字段统计
 * <p>
 * 用于详情页字段旁的变更次数角标，以及按字段筛选的下拉选项
 * </p>
 */
@Data
public class BusinessHistoryFieldStatDto {

    /**
     * 字段名（数据库列名）
     */
    private String field;

    /**
     * 字段中文名
     */
    private String label;

    /**
     * 变更次数
     */
    private long count;
}
