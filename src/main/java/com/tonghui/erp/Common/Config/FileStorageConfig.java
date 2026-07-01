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
        // 设备管理
        put("EQUIPMENT_MAINTENANCE", "设备维保");
        put("EQUIPMENT_PHOTO", "设备照片");
        put("EQUIPMENT_DOCUMENT", "设备文档");
        // 生产管理
        put("PRODUCTION_PLAN", "生产计划");
        put("PRODUCTION_RECORD", "生产记录");
        put("PRODUCTION_PROCESS", "生产工序");
        put("PRODUCTION_REPORT", "生产报告");
        // 制剂管理
        put("PREPARATION_DOCUMENT", "制剂文档");
        put("PREPARATION_FORMULA", "制剂配方");
        put("PREPARATION_SPEC", "制剂规格");
        // 物料管理
        put("MATERIAL_FILE", "物料文件");
        put("MATERIAL_CERTIFICATE", "物料证书");
        // 库存管理
        put("STOCK_IN_PURCHASE", "入库单/原料");
        put("STOCK_IN_AUXILIARY", "入库单/辅料");
        put("STOCK_IN_PACKAGING", "入库单/包材");
        put("STOCK_IN_PRODUCT", "入库单/成品");
        put("STOCK_OUT_SALES", "出库单/销售");
        put("STOCK_OUT_PRODUCTION", "出库单/领料");
        put("STOCK_OUT_RETURN", "出库单/退货");
        // 采购管理
        put("PURCHASE_ORDER", "采购订单");
        put("PURCHASE_CONTRACT", "采购合同");
        put("PURCHASE_INVOICE", "采购发票");
        // 质量管理
        put("QUALITY_RECORD", "质量记录");
        put("QUALITY_INSPECTION", "质检报告");
        put("QUALITY_CERTIFICATE", "质量证书");
        // 人员管理
        put("PERSONNEL_FILE", "人员档案");
        put("PERSONNEL_CERTIFICATE", "人员档案/证书");
        put("PERSONNEL_HEALTH_CERT", "人员档案/健康证");
        put("PERSONNEL_HEALTH_FILE", "人员档案/健康档案");
        put("PERSONNEL_WORK_CERT", "人员档案/工作证");
        put("PERSONNEL_ATTACHMENT", "人员档案/附件");
        put("PERSONNEL_QUALIFICATION", "人员档案/资格证书");
        put("PERSONNEL_EDUCATION", "人员档案/学历证书");
        put("PERSONNEL_TRAINING", "人员档案/培训记录");
        // 车间/环境管理
        put("ROOM_DOCUMENT", "车间文档");
        put("DISINFECTION_RECORD", "消毒记录");
        put("CLEAN_INSPECTION_RECORD", "洁净检测");
        put("TEMPERATURE_HUMIDITY_RECORD", "温湿度记录");
        put("PRESSURE_DIFFERENCE_RECORD", "压差记录");
        put("ENVIRONMENT_LICENSE", "环境许可");
        put("ENVIRONMENT_REPORT", "环境检测");
        // 审批管理
        put("APPROVAL", "审批附件");
        // 供应商/客户管理
        put("SUPPLIER_QUALIFICATION", "供应商资质");
        put("CUSTOMER_QUALIFICATION", "客户资质");
        // 通用
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
