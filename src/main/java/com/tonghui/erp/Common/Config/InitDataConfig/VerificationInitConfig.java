package com.tonghui.erp.Common.Config.InitDataConfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;
import com.tonghui.erp.Common.Config.InitDataConfig.YamlPropertySourceFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证类别初始化配置类
 * <p>
 * 从init-data/verification.yml读取验证类别的配置
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "init-data.verification")
@PropertySources({
    @PropertySource(value = "classpath:init-data/verification.yml", encoding = "UTF-8", factory = YamlPropertySourceFactory.class)
})
public class VerificationInitConfig {

    // region 配置属性
    // ===================================
    // 配置属性
    // ===================================

    /**
     * 是否启用验证类别初始化
     */
    private boolean enabled;

    /**
     * 验证类别列表
     */
    private List<CategoryConfig> categories = new ArrayList<>();

    // endregion

    // region 内部类
    // ===================================
    // 内部类
    // ===================================

    /**
     * 验证类别配置
     */
    @Data
    public static class CategoryConfig {
        /**
         * 类别标识（如 equipment、building 等）
         */
        private String key;

        /**
         * 类别名称（如 设备确认、厂房验证 等）
         */
        private String name;
    }

    // endregion
}
