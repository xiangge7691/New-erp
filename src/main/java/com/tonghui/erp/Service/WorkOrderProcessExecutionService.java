package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;

import java.time.LocalDate;
import java.util.List;

/**
 * 工单工序执行记录服务接口
 */
public interface WorkOrderProcessExecutionService extends IService<WorkOrderProcessExecution> {

    /**
     * 根据工单ID查询工序执行记录列表（含实际参与人）
     *
     * @param workOrderId 工单ID
     * @return 工序执行记录列表
     */
    List<WorkOrderProcessExecution> getByWorkOrderId(Long workOrderId);

    /**
     * 查询已完工（锁定）的工序执行记录
     *
     * @param from        完工日期起（可选）
     * @param to          完工日期止（可选）
     * @param workOrderId 工单ID（可选）
     * @return 已完工记录列表（含参与人、计划信息与生成绩效数）
     */
    List<WorkOrderProcessExecution> listCompleted(LocalDate from, LocalDate to, Long workOrderId);

    /**
     * 批量保存工序执行记录与实际参与人
     *
     * @param workOrderId 工单ID
     * @param executions  工序执行记录列表
     */
    void batchSave(Long workOrderId, List<WorkOrderProcessExecution> executions);

    /**
     * 工序/批次完工：锁定执行记录并生成个人绩效
     *
     * @param executionId 工序执行记录ID
     * @return 完工后的执行记录
     */
    WorkOrderProcessExecution complete(Long executionId);
}
