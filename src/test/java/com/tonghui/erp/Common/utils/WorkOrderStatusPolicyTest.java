package com.tonghui.erp.Common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工单状态策略测试
 * <p>
 * 验证“成品入库即生产完成”的口径：已入库/已归档计入完成且在产为 false；
 * 已放行及更早状态仍在产；异常/不合格/作废为终止状态。
 * </p>
 */
public class WorkOrderStatusPolicyTest {

    /**
     * 已入库、已归档、历史已完成 → 生产完成
     */
    @Test
    public void testProductionDoneStatuses() {
        assertTrue(WorkOrderStatusPolicy.isProductionDone(WorkOrderStatusPolicy.INBOUND), "已入库应为生产完成");
        assertTrue(WorkOrderStatusPolicy.isProductionDone(WorkOrderStatusPolicy.ARCHIVED), "已归档应为生产完成");
        assertTrue(WorkOrderStatusPolicy.isProductionDone(WorkOrderStatusPolicy.COMPLETED), "已完成应为生产完成");
    }

    /**
     * 入库前状态 → 生产未完成
     */
    @Test
    public void testProductionNotDoneBeforeInbound() {
        assertFalse(WorkOrderStatusPolicy.isProductionDone(WorkOrderStatusPolicy.PENDING), "待生产不应为生产完成");
        assertFalse(WorkOrderStatusPolicy.isProductionDone(WorkOrderStatusPolicy.PRODUCING), "生产中不应为生产完成");
        assertFalse(WorkOrderStatusPolicy.isProductionDone(WorkOrderStatusPolicy.PRODUCED), "已生产不应为生产完成");
        assertFalse(WorkOrderStatusPolicy.isProductionDone(WorkOrderStatusPolicy.INSPECTING), "检验中不应为生产完成");
        assertFalse(WorkOrderStatusPolicy.isProductionDone(WorkOrderStatusPolicy.INSPECTED), "已检验不应为生产完成");
        assertFalse(WorkOrderStatusPolicy.isProductionDone(WorkOrderStatusPolicy.RELEASED), "已放行不应为生产完成");
    }

    /**
     * 成品入库后不再计入在产
     */
    @Test
    public void testNotRunningAfterInbound() {
        assertFalse(WorkOrderStatusPolicy.isRunning(WorkOrderStatusPolicy.INBOUND), "已入库不应计入在产");
        assertFalse(WorkOrderStatusPolicy.isRunning(WorkOrderStatusPolicy.ARCHIVED), "已归档不应计入在产");
        assertFalse(WorkOrderStatusPolicy.isRunning(WorkOrderStatusPolicy.COMPLETED), "已完成不应计入在产");
    }

    /**
     * 入库前状态仍计入在产
     */
    @Test
    public void testRunningBeforeInbound() {
        assertTrue(WorkOrderStatusPolicy.isRunning(WorkOrderStatusPolicy.PENDING), "待生产应计入在产");
        assertTrue(WorkOrderStatusPolicy.isRunning(WorkOrderStatusPolicy.PRODUCING), "生产中应计入在产");
        assertTrue(WorkOrderStatusPolicy.isRunning(WorkOrderStatusPolicy.PRODUCED), "已生产应计入在产");
        assertTrue(WorkOrderStatusPolicy.isRunning(WorkOrderStatusPolicy.INSPECTING), "检验中应计入在产");
        assertTrue(WorkOrderStatusPolicy.isRunning(WorkOrderStatusPolicy.INSPECTED), "已检验应计入在产");
        assertTrue(WorkOrderStatusPolicy.isRunning(WorkOrderStatusPolicy.RELEASED), "已放行应计入在产");
    }

    /**
     * 异常/不合格/作废为终止状态，不计入在产
     */
    @Test
    public void testTerminalStatusesNotRunning() {
        assertFalse(WorkOrderStatusPolicy.isRunning(WorkOrderStatusPolicy.ABNORMAL), "异常不应计入在产");
        assertFalse(WorkOrderStatusPolicy.isRunning(WorkOrderStatusPolicy.UNQUALIFIED), "不合格不应计入在产");
        assertFalse(WorkOrderStatusPolicy.isRunning(WorkOrderStatusPolicy.VOIDED), "作废不应计入在产");
        assertFalse(WorkOrderStatusPolicy.isProductionDone(WorkOrderStatusPolicy.ABNORMAL), "异常不应为生产完成");
    }

    /**
     * 状态为空时按在产兜底
     */
    @Test
    public void testNullStatusRunning() {
        assertTrue(WorkOrderStatusPolicy.isRunning(null), "状态为空应兜底为在产");
        assertFalse(WorkOrderStatusPolicy.isProductionDone(null), "状态为空不应为生产完成");
    }
}
