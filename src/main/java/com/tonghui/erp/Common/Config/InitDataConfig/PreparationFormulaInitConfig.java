package com.tonghui.erp.Common.Config.InitDataConfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;
import com.tonghui.erp.Common.Config.InitDataConfig.YamlPropertySourceFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 制剂处方信息初始化配置类
 * <p>
 * 从init-data/preparation-formulas.yml读取制剂处方信息的初始化配置
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "init-data.preparation-formulas")
@PropertySources({
    @PropertySource(value = "classpath:init-data/preparation-formulas.yml", encoding = "UTF-8", factory = YamlPropertySourceFactory.class)
})
public class PreparationFormulaInitConfig {

    // region 配置属性
    // ===================================
    // 配置属性
    // ===================================

    /**
     * 是否启用制剂处方信息初始化
     */
    private boolean enabled;

    /**
     * 制剂处方列表
     */
    private List<FormulaData> data = new ArrayList<>();

    // endregion

    // region 内部类
    // ===================================
    // 内部类
    // ===================================

    /**
     * 制剂处方数据
     */
    @Data
    public static class FormulaData {
        /**
         * 制剂编码
         */
        private String preparationCode;

        /**
         * 物料编号
         */
        private String materialCode;

        /**
         * 物料名称
         */
        private String materialName;

        /**
         * 物料分类（生产原料/辅料/包材）
         */
        private String materialCategory;

        /**
         * 处方量
         */
        private BigDecimal dosage;

        /**
         * 单位名称
         */
        private String unitName;
    }

    // endregion
}
