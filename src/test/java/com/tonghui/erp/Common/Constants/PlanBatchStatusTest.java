package com.tonghui.erp.Common.Constants;

import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 排产批次状态推导测试
 * <p>
 * 验证批次状态按关联执行记录推导的规则：
 * 无执行=计划中，有开始未完成=执行中，全部完成=已完工；
 * 执行记录优先按批次ID关联，未写批次ID时按批次序号兜底。
 * </p>
 */
public class PlanBatchStatusTest {

    /**
     * 无关联执行记录 → 计划中
     */
    @Test
    public void testPlannedWhenNoExecutions() {
        assertEquals(PlanBatchStatus.PLANNED, PlanBatchStatus.resolve(List.of()),
                "无执行记录应为计划中");
        assertEquals(PlanBatchStatus.PLANNED, PlanBatchStatus.resolve(null),
                "执行记录为空应为计划中");
    }

    /**
     * 仅有待执行记录（未填写开始时间）→ 计划中
     */
    @Test
    public void testPlannedWhenExecutionNotStarted() {
        WorkOrderProcessExecution execution = execution(null, null);
        execution.setStatus(ProcessExecutionStatus.PENDING);
        assertEquals(PlanBatchStatus.PLANNED, PlanBatchStatus.resolve(List.of(execution)),
                "执行记录未开始应为计划中");
    }

    /**
     * 已填写开始时间但未完成 → 执行中
     */
    @Test
    public void testInProgressWhenExecutionStarted() {
        WorkOrderProcessExecution execution = execution(LocalDateTime.of(2026, 11, 20, 8, 0), null);
        execution.setStatus(ProcessExecutionStatus.IN_PROGRESS);
        assertEquals(PlanBatchStatus.IN_PROGRESS, PlanBatchStatus.resolve(List.of(execution)),
                "已开始未完成应为执行中");
    }

    /**
     * 多条执行记录部分完成 → 执行中
     */
    @Test
    public void testInProgressWhenPartiallyCompleted() {
        WorkOrderProcessExecution completed = execution(
                LocalDateTime.of(2026, 11, 20, 8, 0), LocalDateTime.of(2026, 11, 20, 10, 0));
        completed.setStatus(ProcessExecutionStatus.COMPLETED);
        WorkOrderProcessExecution inProgress = execution(LocalDateTime.of(2026, 11, 20, 11, 0), null);
        inProgress.setStatus(ProcessExecutionStatus.IN_PROGRESS);
        assertEquals(PlanBatchStatus.IN_PROGRESS,
                PlanBatchStatus.resolve(List.of(completed, inProgress)),
                "多条执行记录未全部完成应为执行中");
    }

    /**
     * 关联执行记录全部完成 → 已完工
     */
    @Test
    public void testCompletedWhenAllExecutionsCompleted() {
        WorkOrderProcessExecution first = execution(
                LocalDateTime.of(2026, 11, 20, 8, 0), LocalDateTime.of(2026, 11, 20, 10, 0));
        first.setStatus(ProcessExecutionStatus.COMPLETED);
        WorkOrderProcessExecution second = execution(
                LocalDateTime.of(2026, 11, 20, 10, 0), LocalDateTime.of(2026, 11, 20, 12, 0));
        second.setStatus(ProcessExecutionStatus.COMPLETED);
        assertEquals(PlanBatchStatus.COMPLETED, PlanBatchStatus.resolve(List.of(first, second)),
                "关联执行全部完成应为已完工");
    }

    /**
     * 历史数据状态未回写，但实际结束时间已填写 → 已完工
     */
    @Test
    public void testCompletedWhenEndTimePresentButStatusStale() {
        WorkOrderProcessExecution execution = execution(
                LocalDateTime.of(2026, 11, 20, 8, 0), LocalDateTime.of(2026, 11, 20, 10, 0));
        execution.setStatus(ProcessExecutionStatus.IN_PROGRESS);
        assertEquals(PlanBatchStatus.COMPLETED, PlanBatchStatus.resolve(List.of(execution)),
                "结束时间已填写应按已完工处理");
    }

    /**
     * 执行记录写入批次ID时按批次ID关联，序号不同也不影响
     */
    @Test
    public void testMatchesByPlanBatchId() {
        WorkOrderProcessPlanBatch batch = batch(146L, 1);
        WorkOrderProcessExecution execution = execution(null, null);
        execution.setPlanBatchId(146L);
        execution.setBatchNo(9);
        assertTrue(PlanBatchStatus.matches(batch, execution), "批次ID一致应判定归属该批次");

        execution.setPlanBatchId(147L);
        assertFalse(PlanBatchStatus.matches(batch, execution), "批次ID不一致不应归属该批次");
    }

    /**
     * 历史执行记录未写批次ID时按批次序号兜底
     */
    @Test
    public void testMatchesByBatchNoFallback() {
        WorkOrderProcessPlanBatch batch = batch(146L, 2);
        WorkOrderProcessExecution execution = execution(null, null);
        execution.setPlanBatchId(null);
        execution.setBatchNo(2);
        assertTrue(PlanBatchStatus.matches(batch, execution), "批次ID为空且序号一致应判定归属该批次");

        execution.setBatchNo(1);
        assertFalse(PlanBatchStatus.matches(batch, execution), "序号不一致不应归属该批次");
    }

    /**
     * 构造执行记录
     */
    private WorkOrderProcessExecution execution(LocalDateTime startTime, LocalDateTime endTime) {
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setStartTime(startTime);
        execution.setEndTime(endTime);
        return execution;
    }

    /**
     * 构造排产批次
     */
    private WorkOrderProcessPlanBatch batch(Long id, Integer batchNo) {
        WorkOrderProcessPlanBatch batch = new WorkOrderProcessPlanBatch();
        batch.setId(id);
        batch.setBatchNo(batchNo);
        return batch;
    }
}
