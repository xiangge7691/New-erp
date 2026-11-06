package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件必传要求配置实体
 * <p>
 * 产品信息汇总的缺件统计、审核放行的文件完整性共用同一份配置。
 * scope + scope_key 支持全局、品种级、被检品属性级覆盖。
 * </p>
 *
 * @TableName file_requirement_config
 */
@TableName(value = "file_requirement_config")
@Data
@EqualsAndHashCode(callSuper = true)
public class FileRequirementConfig extends AuditEntity {

    // region 基本信息
    // ===================================
    // 基本信息
    // ===================================

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 范围：GLOBAL/PREPARATION/ITEM_CATEGORY */
    @TableField(value = "scope")
    private String scope;

    /** 范围值：品种编码/被检品属性 */
    @TableField(value = "scope_key")
    private String scopeKey;

    /** 业务类型 */
    @TableField(value = "biz_type")
    private String bizType;

    /** 文件槽位 */
    @TableField(value = "file_kind")
    private String fileKind;

    /** 要求：REQUIRED/CONDITIONAL/OPTIONAL */
    @TableField(value = "requirement")
    private String requirement;

    /** 环节 */
    @TableField(value = "stage")
    private String stage;

    /** 显示名称 */
    @TableField(value = "display_name")
    private String displayName;

    /** 备注 */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 审计与乐观锁
    // ===================================
    // 审计与乐观锁
    // ===================================

    /** 是否删除：0否/1是 */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /** 乐观锁版本号 */
    @TableField(value = "version")
    private Integer version;

    // endregion
}
