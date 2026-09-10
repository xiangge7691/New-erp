package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工序类型表
 * @TableName process_type
 */
@TableName(value ="process_type")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessType extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 工序类型 ID，主键。唯一标识，自增长。
     */
    @TableId(value = "process_id", type = IdType.AUTO)
    private Integer processId;

    /**
     * 工序类型编码。唯一业务编码，如"ASSEMBLY"。
     */
    @TableField(value = "process_code")
    private String processCode;

    /**
     * 工序类型名称。中文描述，如"提取"。
     */
    @TableField(value = "process_name")
    private String processName;

    // endregion

    // region 业务字段
    // ===================================
    // 业务字段
    // ===================================

    /**
     * 工序类型说明。详细描述，可为空。
     */
    @TableField(value = "process_description")
    private String processDescription;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
    // ===================================

    /**
     * 状态。`1`-启用，`0`-未启用。默认启用。
     */
    @TableField(value = "process_status")
    private Integer processStatus;

    /**
     * 是否已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 乐观锁版本号
     */
    @TableField(value = "version")
    private Integer version;

    // endregion
}
