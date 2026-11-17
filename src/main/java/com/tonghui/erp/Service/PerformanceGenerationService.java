package com.tonghui.erp.Service;

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
}
