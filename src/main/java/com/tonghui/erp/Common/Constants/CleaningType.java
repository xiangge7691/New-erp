package com.tonghui.erp.Common.Constants;

import java.util.List;

/**
 * 清洁类型常量
 * <p>
 * 定义清洁记录中清洁类型的可选值，用于新增/修改时的合法性校验及前端下拉选项展示。
 * 历史数据中公共区清洁曾使用“公区定期清洁消毒”，读取时统一规范化为“公共区定期清洁消毒”。
 * </p>
 */
public class CleaningType {

    // region 清洁类型常量
    // ===================================
    // 清洁类型常量
    // ===================================

    /**
     * 小清场
     */
    public static final String MINOR = "小清场";

    /**
     * 大清场
     */
    public static final String MAJOR = "大清场";

    /**
     * 公共区定期清洁消毒
     */
    public static final String PUBLIC_AREA = "公共区定期清洁消毒";

    /**
     * 历史公共区清洁取值（兼容存量数据）
     */
    private static final String LEGACY_PUBLIC_AREA = "公区定期清洁消毒";

    /**
     * 全部可选清洁类型
     */
    public static final List<String> ALL = List.of(MINOR, MAJOR, PUBLIC_AREA);

    // endregion

    // region 校验方法
    // ===================================
    // 校验方法
    // ===================================

    /**
     * 规范化清洁类型取值
     * <p>
     * 去除前后空格，空字符串返回 null，历史取值“公区定期清洁消毒”转换为“公共区定期清洁消毒”
     * </p>
     *
     * @param type 清洁类型
     * @return 规范化后的取值，空值返回 null
     */
    public static String normalize(String type) {
        if (type == null) {
            return null;
        }
        String trimmed = type.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (LEGACY_PUBLIC_AREA.equals(trimmed)) {
            return PUBLIC_AREA;
        }
        return trimmed;
    }

    /**
     * 判断清洁类型是否为合法取值
     *
     * @param type 清洁类型
     * @return true-合法，false-不合法
     */
    public static boolean isValid(String type) {
        return ALL.contains(normalize(type));
    }

    // endregion
}
