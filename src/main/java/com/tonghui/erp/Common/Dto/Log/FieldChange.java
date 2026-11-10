package com.tonghui.erp.Common.Dto.Log;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段变更项
 * <p>
 * 描述一个字段修改前、修改后的值，用于数据变更日志的字段差异展示
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldChange {

    /**
     * 字段名（数据库列名）
     */
    private String field;

    /**
     * 字段中文名（取列注释，取不到时回退为字段名）
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
}
