package com.tonghui.erp.Common.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 生产用时计算工具类单元测试
 * <p>
 * 覆盖天数计算及无效时间过滤场景，不依赖数据库
 * </p>
 */
public class ProductionDurationUtilsTest {

    // region 生产用时计算
    // ===================================
    // 生产用时计算
    // ===================================

    /**
     * 正常时间区间按秒精确换算为天数并保留两位小数
     */
    @Test
    public void testCalcDays() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 2, 12, 0);

        assertEquals(new BigDecimal("1.50"), ProductionDurationUtils.calcDays(start, end));
    }

    /**
     * 开始或结束为空、结束早于开始时返回 null
     */
    @Test
    public void testCalcDaysInvalid() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 2, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 0, 0);

        assertNull(ProductionDurationUtils.calcDays(null, end));
        assertNull(ProductionDurationUtils.calcDays(start, null));
        assertNull(ProductionDurationUtils.calcDays(start, end));
    }

    // endregion
}
