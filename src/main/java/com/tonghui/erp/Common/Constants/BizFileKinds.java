package com.tonghui.erp.Common.Constants;

/**
 * 文件槽位常量
 * <p>
 * 与 {@link BizFileType} 中声明的槽位保持一致，前端也维护同名常量。
 * 槽位用于区分同一条业务记录下的不同文件用途，例如货物验收的随货清单、发票、检验报告。
 * </p>
 */
public final class BizFileKinds {

    // region 通用槽位
    // ===================================
    // 通用槽位
    // ===================================

    /** 默认槽位 */
    public static final String DEFAULT = "DEFAULT";

    /** 其他文件 */
    public static final String OTHER = "OTHER";

    // endregion

    // region 验收与质量
    // ===================================
    // 验收与质量
    // ===================================

    /** 随货清单 */
    public static final String WAYBILL = "WAYBILL";

    /** 发票 */
    public static final String INVOICE = "INVOICE";

    /** 检验报告 */
    public static final String INSPECTION_REPORT = "INSPECTION_REPORT";

    // endregion

    // region 生产与制剂
    // ===================================
    // 生产与制剂
    // ===================================

    /** 生产批记录 */
    public static final String BATCH_RECORD = "BATCH_RECORD";

    /** 工序附件 */
    public static final String PROCESS_ATTACHMENT = "PROCESS_ATTACHMENT";

    /** 工艺规程 */
    public static final String PROCESS_SPEC = "PROCESS_SPEC";

    /** 工艺规程流程图 */
    public static final String PROCESS_FLOWCHART = "PROCESS_FLOWCHART";

    /** 批记录模板 */
    public static final String BATCH_RECORD_TEMPLATE = "BATCH_RECORD_TEMPLATE";

    /** 验证方案 */
    public static final String VERIFICATION_PLAN = "VERIFICATION_PLAN";

    /** 验证模板 */
    public static final String VERIFICATION_TEMPLATE = "VERIFICATION_TEMPLATE";

    /** 检验模板 */
    public static final String INSPECTION_TEMPLATE = "INSPECTION_TEMPLATE";

    /** 质量标准 */
    public static final String QUALITY_STANDARD = "QUALITY_STANDARD";

    // endregion

    // region 机构与人员
    // ===================================
    // 机构与人员
    // ===================================

    /** 许可证扫描件 */
    public static final String LICENSE_SCAN = "LICENSE_SCAN";

    /** 照片 */
    public static final String PHOTO = "PHOTO";

    /** 人员档案附件 */
    public static final String ATTACHMENT = "ATTACHMENT";

    /** 健康档案 */
    public static final String HEALTH_FILE = "HEALTH_FILE";

    /** 证书文件 */
    public static final String CERTIFICATE_FILE = "CERTIFICATE_FILE";

    // endregion

    // region 车间环境
    // ===================================
    // 车间环境
    // ===================================

    /** 消毒记录 */
    public static final String DISINFECTION = "DISINFECTION";

    /** 洁净检测 */
    public static final String CLEAN_INSPECTION = "CLEAN_INSPECTION";

    /** 清洁记录 */
    public static final String CLEANING_RECORD = "CLEANING_RECORD";

    // endregion

    private BizFileKinds() {
    }
}
