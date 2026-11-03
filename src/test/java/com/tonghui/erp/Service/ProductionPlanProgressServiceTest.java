package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Constants.PurchaseProcessNode;
import com.tonghui.erp.Common.Dto.ProductionPlanOverdueDto;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Service.impl.ProductionPlanProgressServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 生产计划采购进度服务单元测试
 * <p>
 * 覆盖逾期判定和节点进度计算，不依赖数据库
 * </p>
 */
public class ProductionPlanProgressServiceTest {

    // region 逾期判定
    // ===================================
    // 逾期判定
    // ===================================

    /**
     * 计划生产时间和需交付时间都在未来时不逾期
     */
    @Test
    public void testNotOverdueWhenFuture() {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(LocalDateTime.now().plusDays(1));
        plan.setDeliveryTime(LocalDateTime.now().plusDays(2));
        plan.setCurrentStatus("生产中");

        ProductionPlanOverdueDto overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);

        assertFalse(overdue.getPlanProductionTime());
        assertFalse(overdue.getDeliveryTime());
        assertEquals("NONE", overdue.getLevel());
    }

    /**
     * 计划生产时间已过且计划未完成时标记为 PLAN
     */
    @Test
    public void testPlanTimeOverdue() {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(LocalDateTime.now().minusDays(1));
        plan.setDeliveryTime(LocalDateTime.now().plusDays(1));
        plan.setCurrentStatus("生产中");

        ProductionPlanOverdueDto overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);

        assertTrue(overdue.getPlanProductionTime());
        assertFalse(overdue.getDeliveryTime());
        assertEquals("PLAN", overdue.getLevel());
        assertTrue(overdue.getPlanProductionOverdueMinutes() > 0);
    }

    /**
     * 两个时间都已过且计划未完成时标记为 BOTH
     */
    @Test
    public void testBothOverdue() {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(LocalDateTime.now().minusDays(2));
        plan.setDeliveryTime(LocalDateTime.now().minusDays(1));
        plan.setCurrentStatus("生产中");

        ProductionPlanOverdueDto overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);

        assertTrue(overdue.getPlanProductionTime());
        assertTrue(overdue.getDeliveryTime());
        assertEquals("BOTH", overdue.getLevel());
    }

    /**
     * 已完成或已归档的计划不标红
     */
    @Test
    public void testFinishedPlanNotOverdue() {
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(LocalDateTime.now().minusDays(2));
        plan.setDeliveryTime(LocalDateTime.now().minusDays(1));
        plan.setCurrentStatus("已完成");

        ProductionPlanOverdueDto overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);

        assertFalse(overdue.getPlanProductionTime());
        assertFalse(overdue.getDeliveryTime());
        assertEquals("NONE", overdue.getLevel());

        plan.setCurrentStatus("生产中");
        plan.setIsArchived(1);
        overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);
        assertEquals("NONE", overdue.getLevel());
    }

    // endregion

    // region 节点进度
    // ===================================
    // 节点进度
    // ===================================

    /**
     * 六个业务节点按已完成数量均分进度
     */
    @Test
    public void testNodeProgressPercent() {
        assertEquals(0, PurchaseProcessNode.progressPercent(0));
        assertEquals(33, PurchaseProcessNode.progressPercent(2));
        assertEquals(50, PurchaseProcessNode.progressPercent(3));
        assertEquals(100, PurchaseProcessNode.progressPercent(6));
        assertEquals(100, PurchaseProcessNode.progressPercent(9));
    }

    // endregion
}
