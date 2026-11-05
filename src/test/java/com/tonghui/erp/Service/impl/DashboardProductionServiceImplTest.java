package com.tonghui.erp.Service.impl;

import com.tonghui.erp.Data.Entity.ProductionPlan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * 生产计划归属月份解析测试
 * <p>
 * 覆盖「计划生产时间为空时回退创建时间」的口径，
 * 防止待生产计划因计划生产时间未填写被区间统计丢弃。
 * </p>
 */
class DashboardProductionServiceImplTest {

    // region 月份解析
    // ===================================
    // 月份解析
    // ===================================

    /**
     * 优先使用计划生产时间
     */
    @Test
    void resolvePlanMonth_prefersPlanProductionTime() {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(LocalDateTime.of(2026, 9, 16, 8, 0));
        plan.setCreatedTime(LocalDateTime.of(2026, 10, 19, 9, 11));

        Assertions.assertEquals("2026-09", DashboardProductionServiceImpl.resolvePlanMonth(plan));
    }

    /**
     * 计划生产时间为空时回退创建时间
     */
    @Test
    void resolvePlanMonth_fallsBackToCreatedTime() {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(null);
        plan.setCreatedTime(LocalDateTime.of(2026, 11, 2, 15, 55));

        Assertions.assertEquals("2026-11", DashboardProductionServiceImpl.resolvePlanMonth(plan));
    }

    /**
     * 两种时间都为空时返回 null
     */
    @Test
    void resolvePlanMonth_returnsNullWhenBothMissing() {
        ProductionPlan plan = new ProductionPlan();

        Assertions.assertNull(DashboardProductionServiceImpl.resolvePlanMonth(plan));
        Assertions.assertNull(DashboardProductionServiceImpl.resolvePlanMonth(null));
    }

    // endregion
}
