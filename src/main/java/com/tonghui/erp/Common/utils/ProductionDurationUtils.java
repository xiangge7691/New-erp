package com.tonghui.erp.Common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 生产用时计算工具类
 * <p>
 * 统一生产计划、生产任务的生产用时口径：开始时间到结束时间的天数
 * </p>
 */
public final class ProductionDurationUtils {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    /** 一天的秒数 */
    private static final BigDecimal SECONDS_PER_DAY = BigDecimal.valueOf(86400);

    /** 生产用时保留小数位 */
    private static final int DURATION_SCALE = 2;

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 私有构造方法，禁止实例化工具类
     */
    private ProductionDurationUtils() {
    }

    // endregion

    // region 生产用时计算
    // ===================================
    // 生产用时计算
    // ===================================

    /**
     * 计算两个时间之间的生产用时（天）
     * <p>
     * 开始或结束时间为空、结束时间早于开始时间时返回 null
     * </p>
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 生产用时（天，保留两位小数），无效时返回 null
     */
    public static BigDecimal calcDays(LocalDateTime start, LocalDateTime end) {
        if (!isValid(start, end)) {
            return null;
        }
        long seconds = Duration.between(start, end).getSeconds();
        return BigDecimal.valueOf(seconds)
                .divide(SECONDS_PER_DAY, DURATION_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 判断生产用时是否有效
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 两个时间均非空且结束时间不早于开始时间返回 true
     */
    public static boolean isValid(LocalDateTime start, LocalDateTime end) {
        return start != null && end != null && !end.isBefore(start);
    }

    // endregion
}
