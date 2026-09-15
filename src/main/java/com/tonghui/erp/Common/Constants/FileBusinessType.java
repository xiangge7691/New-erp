package com.tonghui.erp.Common.Constants;

/**
 * 文件业务类型常量
 * <p>
 * 定义文件管理中不同业务场景的类型标识符，用于分类和筛选文件操作日志
 * </p>
 */
public class FileBusinessType {

    // region 货物验收相关
    // ===================================
    // 货物验收相关
    // ===================================

    /**
     * 货物验收 - 随货清单
     */
    public static final String GOODS_ACCEPTANCE_WAYBILL = "GOODS_ACCEPTANCE_WAYBILL";

    /**
     * 货物验收 - 发票
     */
    public static final String GOODS_ACCEPTANCE_INVOICE = "GOODS_ACCEPTANCE_INVOICE";

    /**
     * 货物验收 - 检验报告
     */
    public static final String GOODS_ACCEPTANCE_INSPECTION_REPORT = "GOODS_ACCEPTANCE_INSPECTION_REPORT";

    // endregion

    // region 审核放行记录
    // ===================================
    // 审核放行记录
    // ===================================

    /**
     * 审核放行记录
     */
    public static final String AUDIT_RELEASE_RECORD = "AUDIT_RELEASE_RECORD";

    // endregion

    // region 留样记录
    // ===================================
    // 留样记录
    // ===================================

    /**
     * 留样记录
     */
    public static final String SAMPLE_RETENTION_RECORD = "SAMPLE_RETENTION_RECORD";

    // endregion

    // region 通用类型
    // ===================================
    // 通用类型
    // ===================================

    /**
     * 通用文件（无特定业务类型）
     */
    public static final String GENERAL = "GENERAL";

    // endregion
}
