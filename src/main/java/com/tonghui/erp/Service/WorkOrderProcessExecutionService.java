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
     * 批量保存工序执行记录（先删后插）
     *
     * @param workOrderId 工单ID
     * @param executions 工序执行记录列表
     */
    void batchSave(Long workOrderId, List<WorkOrderProcessExecution> executions);
}
