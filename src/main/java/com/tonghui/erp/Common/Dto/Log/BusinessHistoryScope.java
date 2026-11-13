package com.tonghui.erp.Common.Dto.Log;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务变更历史查询范围
 * <p>
 * 由业务 Provider 解析业务对象后给出需要查询的表与主键集合，
 * 以及关联记录的展示名称（如处方行对应的物料名称）
 * </p>
 */
@Data
public class BusinessHistoryScope {

    // region 业务对象字段
    // ===================================
    // 业务对象字段
    // ===================================

    /**
     * 业务对象展示名称
     */
    private String title;

    /**
     * 需要查询变更日志的表范围
     */
    private List<TableScope> tables = new ArrayList<>();

    /**
     * 关联记录名称，key 为 tableName#recordId
     */
    private Map<String, String> recordLabels = new HashMap<>();

    // endregion

    // region 便捷方法
    // ===================================
    // 便捷方法
    // ===================================

    /**
     * 添加一张表的查询范围
     *
     * @param tableName   表名
     * @param businessIds 业务记录主键集合
     */
    public void addTable(String tableName, List<String> businessIds) {
        if (tableName == null || businessIds == null || businessIds.isEmpty()) {
            return;
        }
        tables.add(new TableScope(tableName, businessIds));
    }

    /**
     * 添加关联记录名称
     *
     * @param tableName 表名
     * @param recordId  记录主键
     * @param label     展示名称
     */
    public void addRecordLabel(String tableName, String recordId, String label) {
        if (tableName == null || recordId == null || label == null) {
            return;
        }
        recordLabels.put(recordKey(tableName, recordId), label);
    }

    /**
     * 获取关联记录名称
     *
     * @param tableName 表名
     * @param recordId  记录主键
     * @return 展示名称；未登记时返回 null
     */
    public String recordLabel(String tableName, String recordId) {
        if (tableName == null || recordId == null) {
            return null;
        }
        return recordLabels.get(recordKey(tableName, recordId));
    }

    /**
     * 生成记录名称的键
     *
     * @param tableName 表名
     * @param recordId  记录主键
     * @return 记录名称键
     */
    private String recordKey(String tableName, String recordId) {
        return tableName + "#" + recordId;
    }

    // endregion

    // region 表范围
    // ===================================
    // 表范围
    // ===================================

    /**
     * 单表查询范围
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableScope {

        /**
         * 表名
         */
        private String tableName;

        /**
         * 业务记录主键集合
         */
        private List<String> businessIds;
    }

    // endregion
}
