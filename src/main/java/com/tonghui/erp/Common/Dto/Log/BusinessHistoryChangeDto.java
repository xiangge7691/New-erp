package com.tonghui.erp.Common.Dto.Log;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 业务变更历史中的单个字段变更
 * <p>
 * 一个字段对应一条前端可渲染的改前/改后记录；
 * 附件变更（changeKind=FILE）额外返回文件操作与文件信息
 * </p>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BusinessHistoryChangeDto {

    /**
     * 数据变更日志ID
     */
    private Long dataChangeId;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 变更记录主键值
     */
    private String businessId;

    /**
     * 关联记录名称（如处方行对应的物料名称）
     */
    private String recordLabel;

    /**
     * 变更类型：INSERT/UPDATE/DELETE
     */
    private String changeType;

    /**
     * 变更来源：PRIMARY主记录/RELATED关联变更
     */
    private String changeSource;

    /**
     * 字段名（数据库列名）
     */
    private String field;

    /**
     * 字段中文名
     */
    private String label;

    /**
     * 修改前值
     */
    private Object before;

    /**
     * 修改后值
     */
    private Object after;

    // region 附件变更字段
    // ===================================
    // 附件变更字段
    // ===================================

    /**
     * 变更内容类型：FIELD 字段变更 / FILE 附件变更
     */
    private String changeKind;

    /**
     * 附件操作：ADD 上传 / DELETE 删除
     */
    private String fileAction;

    /**
     * 附件ID
     */
    private Long fileId;

    /**
     * 附件文件名
     */
    private String fileName;

    /**
     * 附件槽位/用途，如 PROCESS_ATTACHMENT
     */
    private String fileKind;

    /**
     * 附件大小（字节）
     */
    private Long fileSize;

    // endregion
}
