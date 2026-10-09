package com.tonghui.erp.Common.Dto.System;

import lombok.Data;

/**
 * 系统配置项数据传输对象
 * <p>
 * 用于配置页展示与编辑单个配置项，包含键、值、名称、分组、值类型与说明
 * </p>
 */
@Data
public class SystemConfigDto {

    // region 配置项字段
    // ===================================
    // 配置项字段
    // ===================================

    /**
     * 配置唯一标识
     */
    private Long configId;

    /**
     * 配置键（唯一）
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置名称（用于配置页展示）
     */
    private String configName;

    /**
     * 配置分组（如 dashboard_expiry）
     */
    private String configGroup;

    /**
     * 值类型：int/string/bool/json
     */
    private String valueType;

    /**
     * 配置说明
     */
    private String remark;

    /**
     * 状态：0停用/1启用
     */
    private Integer status;

    // endregion
}
