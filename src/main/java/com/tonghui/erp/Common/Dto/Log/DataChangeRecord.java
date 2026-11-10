package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据变更记录
 * <p>
 * 拦截器在请求线程中采集的单条数据变更，最终异步写入 sys_data_change_log
 * </p>
 */
@Data
public class DataChangeRecord {

    // region 变更对象字段
    // ===================================
    // 变更对象字段
    // ===================================

    /**
     * 表名
     */
    private String tableName;

    /**
     * 实体类全名
     */
    private String entityClass;

    /**
     * 业务记录主键值
     */
    private String businessId;

    /**
     * 变更类型：INSERT/UPDATE/DELETE
     */
    private String changeType;

    /**
     * 来源：PRIMARY主记录/RELATED关联变更
     */
    private String changeSource = "RELATED";

    // endregion

    // region 变更内容字段
    // ===================================
    // 变更内容字段
    // ===================================

    /**
     * 修改前整行快照
     */
    private String beforeJson;

    /**
     * 修改后整行快照
     */
    private String afterJson;

    /**
     * 字段差异
     */
    private List<FieldChange> changedFields = new ArrayList<>();

    /**
     * 发生变化的字段数
     */
    private Integer fieldCount = 0;

    // endregion
}
