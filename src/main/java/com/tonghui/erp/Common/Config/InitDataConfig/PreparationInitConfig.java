package com.tonghui.erp.Common.Config.InitDataConfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 制剂信息初始化配置类
 * <p>
 * 从init-data/preparations.yml读取制剂信息的初始化配置
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "init-data.preparations")
@PropertySources({
    @PropertySource(value = "classpath:init-data/preparations.yml", encoding = "UTF-8")
})
public class PreparationInitConfig {

    // region 配置属性
    // ===================================
    // 配置属性
    // ===================================

    /**
     * 是否启用制剂信息初始化
     */
    private boolean enabled;

    /**
     * 制剂列表
     */
    private List<PreparationData> data = new ArrayList<>();

    // endregion

    // region 内部类
    // ===================================
    // 内部类
    // ===================================

    /**
     * 制剂数据
     */
    @Data
    public static class PreparationData {
        /**
         * 制剂编码
         */
        private String preparationCode;

        /**
         * 制剂品名
         */
        private String preparationName;

        /**
         * 状态：0禁用/1启用
         */
        private Integer status;
    }

    // endregion
}
