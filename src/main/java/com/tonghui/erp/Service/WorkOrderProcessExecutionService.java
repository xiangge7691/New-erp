package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;

import java.util.List;

/**
 * 工单工序执行记录服务接口
 */
public interface WorkOrderProcessExecutionService extends IService<WorkOrderProcessExecution> {

    /**
     * 根据工单ID查询工序执行记录列表
     *
     * @param workOrderId 工单ID
     * @return 工序执行记录列表
     */
    List<WorkOrderProcessExecution> getByWorkOrderId(Long workOrderId);

    /**
     * 批量保存工序执行记录
     * <p>
     * 已有行按 id 原地更新（保留ID，支持按行关联），无 id 的行新增，
     * 数据库中未被入参覆盖的既有行被删除
     * </p>
     *
     * @param workOrderId 工单ID
     * @param executions  工序执行记录列表（已有行须携带原 id）
     */
    void batchSave(Long workOrderId, List<WorkOrderProcessExecution> executions);
}
