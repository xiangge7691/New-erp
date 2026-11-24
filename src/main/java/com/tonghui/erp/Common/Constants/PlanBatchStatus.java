package com.tonghui.erp.Common.Constants;

import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch;

import java.util.List;
import java.util.Objects;

/**
 * 排产批次状态常量
 * <p>
 * 批次状态不再固定为「计划中」，而是按关联的实际执行记录推导：
 * 无执行记录=计划中，已登记实际生产但未全部完成=执行中，关联执行全部完成=已完工。
 * 执行记录优先按批次ID关联，历史数据未写批次ID时按批次序号兜底。
 * </p>
 */
public final class PlanBatchStatus {

    // region 状态常量
    // ===================================
    // 状态常量
    // ===================================

    /**
     * 计划中（尚未登记实际生产）
     */
    public static final String PLANNED = "计划中";

    /**
     * 执行中（已登记实际生产，但关联执行未全部完成）
     */
    public static final String IN_PROGRESS = "执行中";

    /**
     * 已完工（关联的实际执行记录全部完成）
     */
    public static final String COMPLETED = "已完工";

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 私有构造方法，禁止实例化
     */
    private PlanBatchStatus() {
    }

    // endregion

    // region 状态推导
    // ===================================
    // 状态推导
    // ===================================

    /**
     * 判断执行记录是否归属于指定批次
     * <p>
     * 优先按批次ID关联；历史记录未写批次ID时按批次序号兜底，
     * 因此调用方需保证执行记录已按同一工序过滤，避免跨工序列误匹配。
     * </p>
     *
     * @param batch     排产批次
     * @param execution 工序执行记录
     * @return 是否归属该批次
     */
    public static boolean matches(WorkOrderProcessPlanBatch batch, WorkOrderProcessExecution execution) {
        if (batch == null || execution == null) {
            return false;
        }
        if (execution.getPlanBatchId() != null) {
            return Objects.equals(execution.getPlanBatchId(), batch.getId());
        }
        return batch.getBatchNo() != null && Objects.equals(execution.getBatchNo(), batch.getBatchNo());
    }

    /**
     * 按关联的实际执行记录推导批次状态
     *
     * @param executions 归属于该批次的执行记录
     * @return 计划中 / 执行中 / 已完工
     */
    public static String resolve(List<WorkOrderProcessExecution> executions) {
        if (executions == null || executions.isEmpty()) {
            return PLANNED;
        }
        // 历史数据可能状态未回写，实际结束时间已填写的同样视为完成
        if (executions.stream().allMatch(PlanBatchStatus::isCompleted)) {
            return COMPLETED;
        }
        if (executions.stream().anyMatch(e -> e.getStartTime() != null)) {
            return IN_PROGRESS;
        }
        return PLANNED;
    }

    /**
     * 判断执行记录是否已完成
     * <p>状态为已完成，或已填写实际结束时间（历史数据状态未回写）均视为完成。</p>
     *
     * @param execution 工序执行记录
     * @return 是否已完成
     */
    private static boolean isCompleted(WorkOrderProcessExecution execution) {
        return ProcessExecutionStatus.COMPLETED.equals(execution.getStatus())
                || execution.getEndTime() != null;
    }

    // endregion
}
