package com.tonghui.erp.Common.Config.InitDataConfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件业务类型目录映射配置类
 * <p>
 * 从init-data/file-types.yml读取文件业务类型的目录映射配置
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "init-data.file-types")
@PropertySources({
    @PropertySource(value = "classpath:init-data/file-types.yml", encoding = "UTF-8")
})
public class FileTypeInitConfig {

    // region 配置属性
    // ===================================
    // 配置属性
    // ===================================

    /**
     * 是否启用文件业务类型初始化
     */
    private boolean enabled;

    /**
     * 父类型 -> 顶级中文目录名
     */
    private Map<String, String> parentTypes = new HashMap<>();

    /**
     * 子类型 -> 子目录名（全局统一，不含父目录前缀）
     */
    private Map<String, String> subTypes = new HashMap<>();

    // endregion

    // region 便捷方法
    // ===================================
    // 便捷方法
    // ===================================

    /**
     * 根据业务类型获取完整中文目录路径
     * <p>
     * 解析规则：按下划线拆分，第一段为父类型，剩余为子类型
     * 示例：
     * - "EQUIPMENT" -> "设备管理"
     * - "EQUIPMENT_MAINTENANCE" -> "设备管理/维保"
     * </p>
     *
     * @param businessType 业务类型（如 EQUIPMENT_MAINTENANCE）
     * @return 中文目录路径
     */
    public String getBusinessTypeDir(String businessType) {
        if (businessType == null || businessType.isEmpty()) {
            return "documents";
        }

        String[] parts = businessType.split("_", 2);
        String parentType = parts[0];
        String parentDir = parentTypes.get(parentType);

        if (parentDir == null) {
            return businessType;
        }

        if (parts.length == 1) {
            return parentDir;
        }

        String subType = parts[1];
        String subDir = subTypes.get(subType);
        if (subDir != null) {
            return parentDir + "/" + subDir;
        }

        return parentDir + "/" + subType;
    }

    /**
     * 获取父类型对应的顶级目录名
     *
     * @param businessType 业务类型
     * @return 顶级中文目录名
     */
    public String getParentDir(String businessType) {
        if (businessType == null || businessType.isEmpty()) {
            return "documents";
        }
        String parentType = businessType.split("_")[0];
        return parentTypes.getOrDefault(parentType, parentType);
    }

    /**
     * 获取子类型对应的子目录名
     *
     * @param businessType 业务类型
     * @return 子目录名（不含父目录前缀）
     */
    public String getSubTypeDir(String businessType) {
        if (businessType == null || businessType.isEmpty()) {
            return null;
        }
        String[] parts = businessType.split("_", 2);
        if (parts.length < 2) {
            return null;
        }
        return subTypes.get(parts[1]);
    }

    // endregion
}
