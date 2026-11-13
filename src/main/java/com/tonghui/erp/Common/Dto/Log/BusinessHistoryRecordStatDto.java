package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;

/**
 * 业务变更历史记录统计
 * <p>
 * 用于关联明细行的变更次数角标，如某一行处方明细的处方量调整次数
 * </p>
 */
@Data
public class BusinessHistoryRecordStatDto {

    /**
     * 表名
     */
    private String tableName;

    /**
     * 记录主键值
     */
    private String recordId;

    /**
     * 记录展示名称
     */
    private String label;

    /**
     * 变更次数
     */
    private long count;
}
