package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Constants.PurchaseProcessNode;
import com.tonghui.erp.Common.Dto.ProductionPlanOverdueDto;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Service.impl.ProductionPlanProgressServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        LocalDate today = LocalDate.now();
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(today.plusDays(1).atStartOfDay());
        plan.setDeliveryTime(today.plusDays(2).atStartOfDay());
        plan.setCurrentStatus("生产中");

        ProductionPlanOverdueDto overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);

        assertFalse(overdue.getPlanProductionTime());
        assertFalse(overdue.getDeliveryTime());
        assertEquals(0L, overdue.getPlanProductionOverdueDays());
        assertEquals(0L, overdue.getDeliveryOverdueDays());
        assertNull(overdue.getPlanProductionTimeText());
        assertNull(overdue.getDeliveryTimeText());
        assertNull(overdue.getText());
        assertEquals("NONE", overdue.getLevel());
    }

    /**
     * 计划开始时间已过但尚未开始生产时标记为 PLAN
     */
    @Test
    public void testPlanTimeOverdueBeforeProductionStart() {
        LocalDate today = LocalDate.now();
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(today.minusDays(1).atStartOfDay());
        plan.setDeliveryTime(today.plusDays(1).atStartOfDay());
        plan.setCurrentStatus("待生产");

        ProductionPlanOverdueDto overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);

        assertTrue(overdue.getPlanProductionTime());
        assertFalse(overdue.getDeliveryTime());
        assertEquals(1L, overdue.getPlanProductionOverdueDays());
        assertEquals("实际晚了1天", overdue.getPlanProductionTimeText());
        assertEquals("PLAN", overdue.getLevel());
    }

    /**
     * 已完成计划保留实际开始晚于计划开始的历史逾期天数
     */
    @Test
    public void testCompletedPlanKeepsActualStartDelay() {
        LocalDate today = LocalDate.now();
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(today.minusDays(5).atStartOfDay());
        plan.setProductionStartTime(today.minusDays(2).atStartOfDay());
        plan.setCurrentStatus("已完成");

        ProductionPlanOverdueDto overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);

        assertTrue(overdue.getPlanProductionTime());
        assertEquals(3L, overdue.getPlanProductionOverdueDays());
        assertEquals("实际晚了3天", overdue.getPlanProductionTimeText());
        assertEquals("PLAN", overdue.getLevel());
    }

    /**
     * 实际按计划时间或提前开始生产时，计划生产时间不逾期
     */
    @Test
    public void testStartedOnTimeNotOverdue() {
        LocalDate today = LocalDate.now();
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(today.minusDays(2).atStartOfDay());
        plan.setProductionStartTime(today.minusDays(3).atStartOfDay());
        plan.setCurrentStatus("生产中");

        ProductionPlanOverdueDto overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);

        assertFalse(overdue.getPlanProductionTime());
        assertEquals(0L, overdue.getPlanProductionOverdueDays());
        assertNull(overdue.getPlanProductionTimeText());
        assertEquals("NONE", overdue.getLevel());
    }

    /**
     * 已出库但出库时间晚于需交付时间时，标记为 DELIVERY
     */
    @Test
    public void testDeliveryOverdueAfterOutbound() {
        LocalDate today = LocalDate.now();
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(today.minusDays(5).atStartOfDay());
        plan.setProductionStartTime(today.minusDays(5).atStartOfDay());
        plan.setDeliveryTime(today.minusDays(2).atStartOfDay());
        plan.setOutboundTime(today.minusDays(1).atStartOfDay());
        plan.setCurrentStatus("已完成");

        ProductionPlanOverdueDto overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);

        assertFalse(overdue.getPlanProductionTime());
        assertTrue(overdue.getDeliveryTime());
        assertEquals(1L, overdue.getDeliveryOverdueDays());
        assertEquals("实际晚了1天", overdue.getDeliveryTimeText());
        assertEquals("DELIVERY", overdue.getLevel());
    }

    /**
     * 计划开始和成品出库都延迟时标记为 BOTH
     */
    @Test
    public void testBothOverdue() {
        LocalDate today = LocalDate.now();
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(today.minusDays(5).atStartOfDay());
        plan.setProductionStartTime(today.minusDays(2).atStartOfDay());
        plan.setDeliveryTime(today.minusDays(2).atStartOfDay());
        plan.setOutboundTime(today.minusDays(1).atStartOfDay());
        plan.setCurrentStatus("生产中");

        ProductionPlanOverdueDto overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);

        assertTrue(overdue.getPlanProductionTime());
        assertTrue(overdue.getDeliveryTime());
        assertEquals(3L, overdue.getPlanProductionOverdueDays());
        assertEquals(1L, overdue.getDeliveryOverdueDays());
        assertEquals("实际晚了3天", overdue.getPlanProductionTimeText());
        assertEquals("实际晚了1天", overdue.getDeliveryTimeText());
        assertEquals("计划生产时间实际晚了3天，需交付时间实际晚了1天", overdue.getText());
        assertEquals("BOTH", overdue.getLevel());
    }

    /**
     * 作废计划不参与逾期计算
     */
    @Test
    public void testVoidedPlanNotOverdue() {
        LocalDate today = LocalDate.now();
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(today.minusDays(5).atStartOfDay());
        plan.setDeliveryTime(today.minusDays(5).atStartOfDay());
        plan.setCurrentStatus("作废");

        ProductionPlanOverdueDto overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);

        assertFalse(overdue.getPlanProductionTime());
        assertFalse(overdue.getDeliveryTime());
        assertEquals(0L, overdue.getPlanProductionOverdueDays());
        assertEquals(0L, overdue.getDeliveryOverdueDays());
        assertNull(overdue.getPlanProductionTimeText());
        assertNull(overdue.getDeliveryTimeText());
        assertNull(overdue.getText());
        assertEquals("NONE", overdue.getLevel());
    }

    /**
     * 已归档但缺失生产开始/出库时间时，回退到生产结束时间和归档时间
     */
    @Test
    public void testArchivedPlanFallbackTimes() {
        LocalDate today = LocalDate.now();
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(today.minusDays(3).atStartOfDay());
        plan.setProductionEndTime(today.minusDays(1).atStartOfDay());
        plan.setDeliveryTime(today.minusDays(3).atStartOfDay());
        plan.setArchiveTime(today.minusDays(1).atStartOfDay());
        plan.setCurrentStatus("已归档");
        plan.setIsArchived(1);

        ProductionPlanOverdueDto overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);

        assertTrue(overdue.getPlanProductionTime());
        assertTrue(overdue.getDeliveryTime());
        assertEquals(2L, overdue.getPlanProductionOverdueDays());
        assertEquals(2L, overdue.getDeliveryOverdueDays());
        assertEquals("BOTH", overdue.getLevel());
    }

    /**
     * 同一天内实际时间晚于计划时间不算逾期（按自然日计算）
     */
    @Test
    public void testSameDayNotOverdue() {
        LocalDate today = LocalDate.now();
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanProductionTime(today.atStartOfDay());
        plan.setProductionStartTime(today.atTime(23, 59));
        plan.setDeliveryTime(today.atStartOfDay());
        plan.setOutboundTime(today.atTime(23, 59));
        plan.setCurrentStatus("生产中");

        ProductionPlanOverdueDto overdue = new ProductionPlanProgressServiceImpl().calculateOverdue(plan);

        assertFalse(overdue.getPlanProductionTime());
        assertFalse(overdue.getDeliveryTime());
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
