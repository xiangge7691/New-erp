package com.tonghui.erp.Common.Constants;

import java.util.List;

/**
 * 清洁方式常量
 * <p>
 * 定义房间清洁方式的可选值，用于房间新增/修改时的合法性校验及前端下拉选项展示
 * </p>
 */
public class CleaningMethod {

    // region 清洁方式常量
    // ===================================
    // 清洁方式常量
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
     * 公区定期清洁消毒
     */
    public static final String PUBLIC_AREA = "公区定期清洁消毒";

    /**
     * 全部可选清洁方式
     */
    public static final List<String> ALL = List.of(MINOR, MAJOR, PUBLIC_AREA);

    // endregion

    // region 校验方法
    // ===================================
    // 校验方法
    // ===================================

    /**
     * 判断清洁方式是否为合法取值
     *
     * @param method 清洁方式
     * @return true-合法，false-不合法
     */
    public static boolean isValid(String method) {
        return ALL.contains(method);
    }

    // endregion
}
