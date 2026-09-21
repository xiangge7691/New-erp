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
 * 剂型信息初始化配置类
 * <p>
 * 从init-data/dosage-forms.yml读取剂型信息的初始化配置
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "init-data.dosage-forms")
@PropertySources({
    @PropertySource(value = "classpath:init-data/dosage-forms.yml", encoding = "UTF-8", factory = YamlPropertySourceFactory.class)
})
public class DosageFormInitConfig {

    // region 配置属性
    // ===================================
    // 配置属性
    // ===================================

    /**
     * 是否启用剂型信息初始化
     */
    private boolean enabled;

    /**
     * 剂型列表
     */
    private List<DosageFormData> data = new ArrayList<>();

    // endregion

    // region 内部类
    // ===================================
    // 内部类
    // ===================================

    /**
     * 剂型数据
     */
    @Data
    public static class DosageFormData {
        /**
         * 剂型唯一标识（对应数据库ID，用于判断是否已存在）
         */
        private Long dosageId;

        /**
         * 剂型大类（如片剂、注射剂、胶囊剂等）
         */
        private String dosageCategory;

        /**
         * 剂型名称（具体剂型名称，可为空）
         */
        private String dosageName;

        /**
         * 状态：0禁用/1启用
         */
        private Integer status;
    }

    // endregion
}
