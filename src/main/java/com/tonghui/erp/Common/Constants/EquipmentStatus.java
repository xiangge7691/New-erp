package com.tonghui.erp.Common.Constants;

import java.util.List;

/**
 * 设备状态常量
 * <p>
 * 定义设备台账中设备状态的取值，用于设备使用前的状态校验（仅"正常"设备允许安排生产使用）。
 * </p>
 */
public class EquipmentStatus {

    // region 设备状态常量
    // ===================================
    // 设备状态常量
    // ===================================

    /**
     * 正常
     */
    public static final String NORMAL = "正常";

    /**
     * 停用
     */
    public static final String DISABLED = "停用";

    /**
     * 报废
     */
    public static final String SCRAPPED = "报废";

    /**
     * 全部可选设备状态
     */
    public static final List<String> ALL = List.of(NORMAL, DISABLED, SCRAPPED);

    // endregion

    // region 校验方法
    // ===================================
    // 校验方法
    // ===================================

    /**
     * 判断设备状态是否允许用于生产
     *
     * @param status 设备状态
     * @return true-允许使用（正常），false-不允许使用
     */
    public static boolean isUsable(String status) {
        return status != null && NORMAL.equals(status.trim());
    }

    // endregion
}
