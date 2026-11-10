package com.tonghui.erp.Common.Constants;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 工序执行状态推导测试
 * <p>
 * 验证按开始/结束时间推导工序状态的规则：
 * 均有=已完成，仅开始=执行中，均无=待执行，仅结束=待执行兜底。
 * </p>
 */
public class ProcessExecutionStatusTest {

    /**
     * 开始与结束时间齐全 → 已完成
     */
    @Test
    public void testCompletedWhenBothTimesPresent() {
        String status = ProcessExecutionStatus.resolve(
                LocalDateTime.of(2026, 11, 10, 8, 0),
                LocalDateTime.of(2026, 11, 10, 12, 0));
        assertEquals(ProcessExecutionStatus.COMPLETED, status, "起止时间齐全应为已完成");
    }

    /**
     * 仅有开始时间 → 执行中
     */
    @Test
    public void testInProgressWhenOnlyStartTimePresent() {
        String status = ProcessExecutionStatus.resolve(
                LocalDateTime.of(2026, 11, 10, 8, 0), null);
        assertEquals(ProcessExecutionStatus.IN_PROGRESS, status, "仅有开始时间应为执行中");
    }

    /**
     * 均未填写 → 待执行
     */
    @Test
    public void testPendingWhenNoTimes() {
        assertEquals(ProcessExecutionStatus.PENDING,
                ProcessExecutionStatus.resolve(null, null), "均未填写应为待执行");
    }

    /**
     * 仅有结束时间（历史非法数据）→ 待执行兜底
     */
    @Test
    public void testPendingWhenOnlyEndTimePresent() {
        String status = ProcessExecutionStatus.resolve(
                null, LocalDateTime.of(2026, 11, 10, 12, 0));
        assertEquals(ProcessExecutionStatus.PENDING, status, "仅有结束时间应按待执行兜底");
    }
}
