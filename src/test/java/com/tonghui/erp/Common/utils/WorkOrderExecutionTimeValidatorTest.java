package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.ProcessTypeMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 工序实际执行时间校验测试
 * <p>
 * 覆盖任务时间窗（配置时间 ~ 配置完成时间）与工序先后两类规则，
 * 并验证本次更新的原记录会被排除在比较之外。
 * </p>
 */
public class WorkOrderExecutionTimeValidatorTest {

    /** 工序执行记录 Mapper（Mock） */
    private WorkOrderProcessExecutionMapper executionMapper;

    /** 工序类型 Mapper（Mock） */
    private ProcessTypeMapper processTypeMapper;

    /** 待测试的校验器 */
    private WorkOrderExecutionTimeValidator validator;

    @BeforeEach
    public void setUp() {
        executionMapper = mock(WorkOrderProcessExecutionMapper.class);
        processTypeMapper = mock(ProcessTypeMapper.class);
        when(executionMapper.selectList(any())).thenReturn(List.of());
        when(processTypeMapper.selectList(any())).thenReturn(List.of());
        validator = new WorkOrderExecutionTimeValidator(executionMapper, processTypeMapper);
        ReflectionTestUtils.setField(validator, "enabled", true);
    }

    /**
     * 实际开始早于任务配置时间 → 拒绝
     */
    @Test
    public void testStartBeforeConfigDate() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), time(2026, 11, 13, 0, 0));
        WorkOrderProcessExecution row = execution(1L, 1, time(2026, 11, 10, 23, 0), null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(order, List.of(row), Set.of()));
        assertTrue(ex.getMessage().contains("早于任务配置时间"), ex.getMessage());
    }

    /**
     * 实际结束晚于任务配置完成时间 → 拒绝
     */
    @Test
    public void testEndAfterConfigCompleteTime() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), time(2026, 11, 13, 0, 0));
        WorkOrderProcessExecution row = execution(1L, 1,
                time(2026, 11, 12, 22, 0), time(2026, 11, 13, 2, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(order, List.of(row), Set.of()));
        assertTrue(ex.getMessage().contains("晚于任务配置完成时间"), ex.getMessage());
    }

    /**
     * 未填配置完成时间时只校验下界：晚于配置时间的实际时间放行
     */
    @Test
    public void testOnlyLowerBoundWhenConfigCompleteTimeMissing() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), null);
        WorkOrderProcessExecution row = execution(1L, 1,
                time(2026, 11, 20, 8, 0), time(2026, 11, 20, 10, 0));

        assertDoesNotThrow(() -> validator.validate(order, List.of(row), Set.of()));
    }

    /**
     * 实际开始早于上一道工序的实际结束 → 拒绝
     */
    @Test
    public void testStartBeforePreviousStepEnd() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), time(2026, 11, 30, 0, 0));
        WorkOrderProcessExecution previous = execution(10L, 1,
                time(2026, 11, 11, 8, 0), time(2026, 11, 11, 12, 0));
        when(executionMapper.selectList(any())).thenReturn(List.of(previous));
        WorkOrderProcessExecution row = execution(11L, 2,
                time(2026, 11, 11, 11, 0), time(2026, 11, 11, 13, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(order, List.of(row), Set.of()));
        assertTrue(ex.getMessage().contains("早于上一道工序"), ex.getMessage());
    }

    /**
     * 上一道工序已开始未结束（执行中）→ 不允许开始当前工序
     */
    @Test
    public void testPreviousStepUnfinished() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), time(2026, 11, 30, 0, 0));
        WorkOrderProcessExecution previous = execution(10L, 1, time(2026, 11, 11, 8, 0), null);
        when(executionMapper.selectList(any())).thenReturn(List.of(previous));
        WorkOrderProcessExecution row = execution(11L, 2,
                time(2026, 11, 11, 9, 0), time(2026, 11, 11, 10, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(order, List.of(row), Set.of()));
        assertTrue(ex.getMessage().contains("尚未完工"), ex.getMessage());
    }

    /**
     * 合法时间（工序递增、在配置窗口内）→ 放行
     */
    @Test
    public void testValidTimesPass() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), time(2026, 11, 13, 0, 0));
        WorkOrderProcessExecution previous = execution(10L, 1,
                time(2026, 11, 11, 8, 0), time(2026, 11, 11, 12, 0));
        when(executionMapper.selectList(any())).thenReturn(List.of(previous));
        WorkOrderProcessExecution row = execution(11L, 2,
                time(2026, 11, 11, 12, 0), time(2026, 11, 11, 14, 0));

        assertDoesNotThrow(() -> validator.validate(order, List.of(row), Set.of()));
    }

    /**
     * 本次更新的原记录不参与比较：旧值越界不应拦截新值
     */
    @Test
    public void testExcludedRowIgnored() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), time(2026, 11, 13, 0, 0));
        // 原记录的开始时间在窗口外，但本次会被覆盖，故不参与比较
        WorkOrderProcessExecution oldRow = execution(20L, 1,
                time(2026, 11, 1, 8, 0), time(2026, 11, 1, 12, 0));
        when(executionMapper.selectList(any())).thenReturn(List.of(oldRow));
        WorkOrderProcessExecution row = execution(20L, 1,
                time(2026, 11, 11, 8, 0), time(2026, 11, 11, 12, 0));

        assertDoesNotThrow(() -> validator.validate(order, List.of(row), Set.of(20L)));
    }

    /**
     * 同工序不同批次的执行记录不互相约束（允许批次排产时段重叠）
     */
    @Test
    public void testBatchesOfSameStepDoNotConflict() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), time(2026, 11, 13, 0, 0));
        WorkOrderProcessExecution batchOne = execution(30L, 3,
                time(2026, 11, 11, 8, 0), time(2026, 11, 11, 10, 0));
        when(executionMapper.selectList(any())).thenReturn(List.of(batchOne));
        WorkOrderProcessExecution batchTwo = execution(31L, 3,
                time(2026, 11, 11, 9, 0), time(2026, 11, 11, 11, 0));

        assertDoesNotThrow(() -> validator.validate(order, List.of(batchTwo), Set.of()));
    }

    /**
     * 前序工序有执行记录但从未开工 → 不允许开始后道
     */
    @Test
    public void testPreviousStepNotStarted() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), time(2026, 11, 30, 0, 0));
        WorkOrderProcessExecution previous = execution(10L, 1, null, null);
        when(executionMapper.selectList(any())).thenReturn(List.of(previous));
        WorkOrderProcessExecution row = execution(11L, 2,
                time(2026, 11, 11, 9, 0), time(2026, 11, 11, 10, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(order, List.of(row), Set.of()));
        assertTrue(ex.getMessage().contains("尚未登记实际开始时间"), ex.getMessage());
    }

    /**
     * 实际结束早于实际开始 → 拒绝（避免 0 或负工时）
     */
    @Test
    public void testEndNotAfterStart() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), time(2026, 11, 30, 0, 0));
        WorkOrderProcessExecution row = execution(1L, 1,
                time(2026, 11, 11, 12, 0), time(2026, 11, 11, 11, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(order, List.of(row), Set.of()));
        assertTrue(ex.getMessage().contains("必须晚于实际开始"), ex.getMessage());
    }

    /**
     * 修改前序工序结束时间：晚于后续工序已登记的实际开始 → 拒绝
     */
    @Test
    public void testEndAfterNextStepStart() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), time(2026, 11, 30, 0, 0));
        WorkOrderProcessExecution nextStep = execution(11L, 2,
                time(2026, 11, 11, 13, 0), null);
        when(executionMapper.selectList(any())).thenReturn(List.of(nextStep));
        WorkOrderProcessExecution row = execution(10L, 1,
                time(2026, 11, 11, 8, 0), time(2026, 11, 11, 14, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(order, List.of(row), Set.of(10L)));
        assertTrue(ex.getMessage().contains("晚于后续工序"), ex.getMessage());
    }

    /**
     * 只填结束时间（无开始）也要校验任务时间窗，不能因缺少开始时间跳过
     */
    @Test
    public void testEndOnlyRowStillChecksTaskWindow() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), null);
        WorkOrderProcessExecution row = execution(1L, 1,
                null, time(2026, 11, 10, 9, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(order, List.of(row), Set.of()));
        assertTrue(ex.getMessage().contains("早于任务配置时间"), ex.getMessage());
    }

    /**
     * 完工校验：不重复校验前序工序（开工时已校验），但仍拦截晚于后续工序已开始
     */
    @Test
    public void testCompletionStillChecksNextStepStart() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), time(2026, 11, 30, 0, 0));
        WorkOrderProcessExecution previousOpen = execution(5L, 1, time(2026, 11, 11, 8, 0), null);
        WorkOrderProcessExecution nextStep = execution(7L, 3, time(2026, 11, 11, 20, 0), null);
        when(executionMapper.selectList(any())).thenReturn(List.of(previousOpen, nextStep));
        WorkOrderProcessExecution row = execution(10L, 2,
                time(2026, 11, 11, 9, 0), time(2026, 11, 11, 21, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validateCompletion(order, row));
        assertTrue(ex.getMessage().contains("晚于后续工序"), ex.getMessage());
    }

    /**
     * 完工校验：前序工序仍在执行中时，补结束时间不应被前序校验误拦
     */
    @Test
    public void testCompletionIgnoresPreviousOpenStep() {
        WorkOrder order = order(time(2026, 11, 11, 0, 0), time(2026, 11, 30, 0, 0));
        WorkOrderProcessExecution previousOpen = execution(5L, 1, time(2026, 11, 11, 8, 0), null);
        when(executionMapper.selectList(any())).thenReturn(List.of(previousOpen));
        WorkOrderProcessExecution row = execution(10L, 2,
                time(2026, 11, 11, 9, 0), time(2026, 11, 11, 10, 0));

        assertDoesNotThrow(() -> validator.validateCompletion(order, row));
    }

    // region 测试数据构造
    // ===================================
    // 测试数据构造
    // ===================================

    /**
     * 构造工单（只设置配置时间与配置完成时间）
     */
    private WorkOrder order(LocalDateTime configDate, LocalDateTime configCompleteTime) {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(1L);
        order.setConfigDate(configDate);
        order.setConfigCompleteTime(configCompleteTime);
        return order;
    }

    /**
     * 构造工序执行记录
     */
    private WorkOrderProcessExecution execution(Long id, Integer stepOrder,
                                                LocalDateTime start, LocalDateTime end) {
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setId(id);
        execution.setWorkOrderId(1L);
        execution.setStepOrder(stepOrder);
        execution.setProcessTypeId(stepOrder == null ? null : stepOrder.longValue());
        execution.setStartTime(start);
        execution.setEndTime(end);
        return execution;
    }

    /**
     * 构造时间
     */
    private LocalDateTime time(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute);
    }

    // endregion
}
