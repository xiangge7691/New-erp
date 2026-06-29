package com.tonghui.erp.Common.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

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
}
