package com.tonghui.erp.Common.Constants;

import java.time.LocalDateTime;

/**
 * 工单工序执行状态常量
 * <p>
 * 工序执行状态不再依赖前端传值，统一由开始/结束时间推导：
 * 开始与结束时间齐全=已完成，仅有开始时间=执行中，均未填写=待执行。
 * 结束时间单独存在属于非法数据，保存时由校验拦截，读取时按待执行兜底。
 * </p>
 */
public final class ProcessExecutionStatus {

    // region 状态常量
    // ===================================
    // 状态常量
    // ===================================

    /**
     * 待执行（未填写开始/结束时间）
     */
    public static final String PENDING = "待执行";

    /**
     * 执行中（已填写开始时间、未填写结束时间）
     */
    public static final String IN_PROGRESS = "执行中";

    /**
     * 已完成（开始时间与结束时间均已填写）
     */
    public static final String COMPLETED = "已完成";

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 私有构造方法，禁止实例化
     */
    private ProcessExecutionStatus() {
    }

    // endregion

    // region 状态推导
    // ===================================
    // 状态推导
    // ===================================

    /**
     * 根据开始/结束时间推导工序执行状态
     *
     * @param startTime 工序开始时间
     * @param endTime   工序结束时间
     * @return 待执行/执行中/已完成
     */
    public static String resolve(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) {
            // 结束时间单独存在属历史非法数据，按待执行兜底
            return PENDING;
        }
        return endTime == null ? IN_PROGRESS : COMPLETED;
    }

    // endregion
}
