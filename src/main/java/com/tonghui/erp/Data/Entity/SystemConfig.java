package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置表
 * <p>
 * 以键值对形式保存系统运行时配置（如首页到期提醒时间范围等），
 * 支持按分组管理、值类型标注，供配置页展示与编辑
 * </p>
 *
 * @TableName system_config
 */
@TableName(value = "system_config")
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemConfig extends AuditEntity {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 配置唯一标识
     */
    @TableId(value = "config_id", type = IdType.AUTO)
    private Long configId;

    /**
     * 配置键（唯一约束）
     */
    @TableField(value = "config_key")
    private String configKey;

    /**
     * 配置值
     */
    @TableField(value = "config_value")
    private String configValue;

    /**
     * 配置名称（用于配置页展示）
     */
    @TableField(value = "config_name")
    private String configName;

    /**
     * 配置分组（如 dashboard_expiry）
     */
    @TableField(value = "config_group")
    private String configGroup;

    /**
     * 值类型：int/string/bool/json
     */
    @TableField(value = "value_type")
    private String valueType;

    /**
     * 配置说明
     */
    @TableField(value = "remark")
    private String remark;

    // endregion

    // region 状态与审计字段
    // ===================================
    // 状态与审计字段
    // ===================================

    /**
     * 状态：0停用/1启用
     */
    @TableField(value = "status")
    private Integer status;

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
