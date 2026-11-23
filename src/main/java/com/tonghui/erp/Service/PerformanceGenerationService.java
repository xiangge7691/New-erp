package com.tonghui.erp.Service;

import java.time.LocalDate;

/**
 * 绩效生成服务接口
 */
public interface PerformanceGenerationService {

    /**
     * 按工序执行记录与实际参与人生成个人绩效记录
     * <p>幂等：同一执行记录同一人员只生成一条系统记录。</p>
     *
     * @param executionId 工序执行记录ID
     * @return 生成的记录数
     */
    int generateForExecution(Long executionId);

    /**
     * 按完工日期区间补生成绩效记录
     * <p>用于历史执行记录（PDA 结束打点、实际生产保存等）未在完工时生成绩效的场景，
     * 已有绩效记录的执行记录会被自动跳过，可重复调用。</p>
     *
     * @param from 完工日期起（可为空）
     * @param to   完工日期止（可为空）
     * @return 新生成的记录数
     */
    int generateForRange(LocalDate from, LocalDate to);
}
