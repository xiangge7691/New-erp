package com.tonghui.erp.Common.Config;

import com.tonghui.erp.Common.Config.InitDataConfig.FileTypeInitConfig;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 文件存储配置类
 * <p>
 * 配置文件存储路径、大小限制、允许类型等
 * 业务类型目录映射已迁移到init-data/file-types.yml，通过FileTypeInitConfig读取
 * 支持通过application.yml或环境变量进行覆盖
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageConfig {

    // region 字段定义
    // ===================================
    // 字段定义
    // ===================================

    /**
     * 文件业务类型目录映射配置
     */
    private final FileTypeInitConfig fileTypeConfig;

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    @Autowired
    public FileStorageConfig(FileTypeInitConfig fileTypeConfig) {
        this.fileTypeConfig = fileTypeConfig;
    }

    // endregion

    // region 配置属性
    // ===================================
    // 配置属性
    // ===================================

    /**
     * 文件存储基础路径
     * 支持相对路径和绝对路径
     * 可通过环境变量 FILE_STORAGE_PATH 覆盖
     */
    private String basePath = "./uploaded-files";

    /**
     * 自定义文件目录名
     * 与 basePath 同级，用于存放用户自定义文件
     */
    private String customDir = "custom-files";

    /**
     * 最大文件大小（字节）
     * 默认10MB
     */
    private long maxSize = 10 * 1024 * 1024;

    /**
     * 允许的文件类型列表
     */
    private List<String> allowedTypes = Arrays.asList(
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/bmp",
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain",
        "application/zip",
        "application/x-rar-compressed"
    );

    /**
     * 是否启用MD5校验
     */
    private boolean enableMd5Check = true;

    /**
     * 临时文件目录名
     */
    private String tempDir = "temp";

    /**
     * 永久文件目录名
     */
    private String permanentDir = "permanent";

    /**
     * 图片文件子目录
     */
    private String imageDir = "images";

    /**
     * 文档文件子目录
     */
    private String documentDir = "documents";

    /**
     * 归档文件子目录
     */
    private String archiveDir = "archives";

    // endregion

    // region 目录解析方法（委托给FileTypeInitConfig）
    // ===================================
    // 目录解析方法（委托给FileTypeInitConfig）
    // ===================================

    /**
     * 根据业务类型获取完整中文目录路径
     * <p>
     * 解析规则：按下划线拆分，第一段为父类型，剩余为子类型
     * 示例：
     * - "EQUIPMENT" → "设备管理"
     * - "EQUIPMENT_MAINTENANCE" → "设备管理/维保"
     * - "PERSONNEL_CERTIFICATE" → "人员管理/证书"
     * - "STOCK_IN_PURCHASE" → "库存管理/入库单/原料"
     * </p>
     *
     * @param businessType 业务类型（如 EQUIPMENT_MAINTENANCE）
     * @return 中文目录路径
     */
    public String getBusinessTypeDir(String businessType) {
        if (businessType == null || businessType.isEmpty()) {
            return documentDir;
        }
        return fileTypeConfig.getBusinessTypeDir(businessType);
    }

    /**
     * 获取父类型对应的顶级目录名
     *
     * @param businessType 业务类型
     * @return 顶级中文目录名
     */
    public String getParentDir(String businessType) {
        if (businessType == null || businessType.isEmpty()) {
            return documentDir;
        }
        return fileTypeConfig.getParentDir(businessType);
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
        return fileTypeConfig.getSubTypeDir(businessType);
    }

    /**
     * 获取自定义文件的完整路径
     * 与 basePath 同级目录
     *
     * @return 自定义文件目录完整路径
     */
    public String getCustomPath() {
        java.nio.file.Path basePathObj = java.nio.file.Paths.get(basePath).toAbsolutePath().normalize();
        java.nio.file.Path parent = basePathObj.getParent();
        if (parent == null) {
            return "./" + customDir;
        }
        return parent.resolve(customDir).toString();
    }

    // endregion
}
