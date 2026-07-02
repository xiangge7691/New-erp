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

    //#region 业务类型分级映射
    // ===================================
    // 业务类型分级映射
    // ===================================

    /**
     * 父类型 -> 顶级中文目录名
     * 命名规范：{父类型}_{子类型}
     * 目录结构：{顶级目录}/{子目录}
     */
    private Map<String, String> businessTypeDirMap = new HashMap<>() {{
        put("EQUIPMENT", "设备管理");
        put("PRODUCTION", "生产管理");
        put("PREPARATION", "制剂管理");
        put("MATERIAL", "物料管理");
        put("STOCK", "库存管理");
        put("PURCHASE", "采购管理");
        put("QUALITY", "质量管理");
        put("PERSONNEL", "人员管理");
        put("ROOM", "车间环境");
        put("ENVIRONMENT", "环境管理");
        put("APPROVAL", "审批管理");
        put("SUPPLIER", "供应商管理");
        put("CUSTOMER", "客户管理");
        put("GENERAL", "通用文件");
    }};

    /**
     * 子类型 -> 子目录名（全局统一，不含父目录前缀）
     * 完整目录 = 父目录 + "/" + 子目录
     */
    private Map<String, String> subTypeDirMap = new HashMap<>() {{
        put("MAINTENANCE", "维保");
        put("PHOTO", "照片");
        put("DOCUMENT", "文档");
        put("PLAN", "计划");
        put("RECORD", "记录");
        put("PROCESS", "工序");
        put("REPORT", "报告");
        put("FORMULA", "配方");
        put("SPEC", "规格");
        put("FILE", "文件");
        put("CERTIFICATE", "证书");
        put("IN_PURCHASE", "入库单/原料");
        put("IN_AUXILIARY", "入库单/辅料");
        put("IN_PACKAGING", "入库单/包材");
        put("IN_PRODUCT", "入库单/成品");
        put("OUT_SALES", "出库单/销售");
        put("OUT_PRODUCTION", "出库单/领料");
        put("OUT_RETURN", "出库单/退货");
        put("ORDER", "订单");
        put("CONTRACT", "合同");
        put("INVOICE", "发票");
        put("INSPECTION", "质检");
        put("ATTACHMENT", "附件");
        put("CLEAN_INSPECTION", "洁净检测");
        put("CLEANING_RECORD", "清洁记录");
        put("TEMPERATURE_HUMIDITY", "温湿度记录");
        put("PRESSURE_DIFFERENCE", "压差记录");
        put("DISINFECTION", "消毒记录");
        put("LICENSE", "许可");
    }};

    //#endregion

    //#region 目录解析方法
    // ===================================
    // 目录解析方法
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

        String[] parts = businessType.split("_", 2);
        String parentType = parts[0];
        String parentDir = businessTypeDirMap.get(parentType);

        if (parentDir == null) {
            return businessType;
        }

        if (parts.length == 1) {
            return parentDir;
        }

        String subType = parts[1];
        String subDir = subTypeDirMap.get(subType);
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
            return documentDir;
        }
        String parentType = businessType.split("_")[0];
        return businessTypeDirMap.getOrDefault(parentType, parentType);
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
        return subTypeDirMap.get(parts[1]);
    }

    //#endregion
}
