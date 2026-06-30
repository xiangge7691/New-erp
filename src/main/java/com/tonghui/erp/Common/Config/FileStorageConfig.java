package com.tonghui.erp.Common.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageConfig {

    /**
     * 文件存储基础路径
     * 支持相对路径和绝对路径
     * 可通过环境变量 FILE_STORAGE_PATH 覆盖
     */
    private String basePath = "./uploaded-files";

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

    /**
     * 业务类型 -> 中文目录名映射
     */
    private Map<String, String> businessTypeDirMap = new HashMap<>() {{
        put("EQUIPMENT_MAINTENANCE", "维保记录");
        put("EQUIPMENT_PHOTO", "设备照片");
        put("PREPARATION_DOCUMENT", "制剂文档");
        put("PRODUCTION_RECORD", "生产记录");
        put("PRODUCTION_PLAN", "生产计划");
        put("MATERIAL_FILE", "物料文件");
        put("QUALITY_RECORD", "质量记录");
        put("GENERAL", "通用文件");
    }};

    /**
     * 根据业务类型获取中文目录名，未映射时返回业务类型本身
     */
    public String getBusinessTypeDir(String businessType) {
        if (businessType == null || businessType.isEmpty()) {
            return documentDir;
        }
        return businessTypeDirMap.getOrDefault(businessType, businessType);
    }
}
