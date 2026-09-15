package com.tonghui.erp.Common.Config.InitDataConfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 计量单位初始化配置类
 * <p>
 * 从init-data/units.yml读取计量单位的初始化配置
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "init-data.units")
@PropertySources({
    @PropertySource(value = "classpath:init-data/units.yml", encoding = "UTF-8")
})
public class UnitInitConfig {

    // region 配置属性
    // ===================================
    // 配置属性
    // ===================================

    /**
     * 是否启用计量单位初始化
     */
    private boolean enabled;

    /**
     * 计量单位列表
     */
    private List<UnitData> data = new ArrayList<>();

    // endregion

    // region 内部类
    // ===================================
    // 内部类
    // ===================================

    /**
     * 计量单位数据
     */
    @Data
    public static class UnitData {
        /**
         * 单位中文名称
         */
        private String unitName;

        /**
         * 单位符号
         */
        private String symbol;

        /**
         * 状态：0禁用/1启用
         */
        private Integer status;
    }

    // endregion
}
