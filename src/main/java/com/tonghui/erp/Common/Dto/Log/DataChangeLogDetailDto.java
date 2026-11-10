package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据变更日志详情
 * <p>
 * 在列表字段基础上补充整行修改前后 JSON，用于查看完整快照
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DataChangeLogDetailDto extends DataChangeLogDto {

    // region 整行快照字段
    // ===================================
    // 整行快照字段
    // ===================================

    /**
     * 修改前整行快照
     */
    private String beforeJson;

    /**
     * 修改后整行快照
     */
    private String afterJson;

    // endregion
}
