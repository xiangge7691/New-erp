package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Common.utils.EntityUtils;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import com.tonghui.erp.Service.WorkOrderProcessExecutionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单工序执行记录服务实现类
 */
@Service
public class WorkOrderProcessExecutionServiceImpl extends ServiceImpl<WorkOrderProcessExecutionMapper, WorkOrderProcessExecution>
        implements WorkOrderProcessExecutionService {

    @Override
    public List<WorkOrderProcessExecution> getByWorkOrderId(Long workOrderId) {
        return baseMapper.selectByWorkOrderIdWithDetails(workOrderId);
    }

    @Override
    @Transactional
    public void batchSave(Long workOrderId, List<WorkOrderProcessExecution> executions) {
        // 先物理删除原有记录
        baseMapper.physicalDeleteByWorkOrderId(workOrderId);

        // 批量插入新记录
        if (executions != null && !executions.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            Long currentUserId = EntityUtils.getCurrentUserId();

            for (WorkOrderProcessExecution execution : executions) {
                execution.setId(null);
                execution.setWorkOrderId(workOrderId);
                execution.setIsDeleted(0);
                execution.setVersion(1);
                execution.setCreatedBy(currentUserId);
                execution.setUpdatedBy(currentUserId);
                execution.setCreatedTime(now);
                execution.setUpdatedTime(now);
            }
            this.saveBatch(executions);
        }
    }
}
