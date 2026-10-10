package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import com.tonghui.erp.Service.WorkOrderProcessExecutionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工单工序执行记录服务实现类
 */
@Service
public class WorkOrderProcessExecutionServiceImpl extends ServiceImpl<WorkOrderProcessExecutionMapper, WorkOrderProcessExecution>
        implements WorkOrderProcessExecutionService {

    // region 查询操作
    // ===================================
    // 查询操作
    // ===================================

    @Override
    public List<WorkOrderProcessExecution> getByWorkOrderId(Long workOrderId) {
        return baseMapper.selectByWorkOrderIdWithDetails(workOrderId);
    }

    // endregion

    // region 批量操作
    // ===================================
    // 批量操作
    // ===================================

    /**
     * 批量保存工序执行记录
     * <p>
     * 使用事务保证数据一致性。为支持按行关联（如按行附件 file_info.business_id = 执行记录id），
     * 采用原地更新保留ID的策略：入参中携带 id 且属于该工单的已有行执行更新（ID不变），
     * 无 id 的行新增，数据库中未被入参覆盖的既有行被删除。
     * </p>
     *
     * @param workOrderId 工单ID
     * @param executions  工序执行记录列表（已有行须携带原 id，可为null或空列表将清空所有记录）
     */
    @Override
    @Transactional
    public void batchSave(Long workOrderId, List<WorkOrderProcessExecution> executions) {
        // 查询该工单现有记录（用于保留ID与计算被移除行）
        Map<Long, WorkOrderProcessExecution> existingMap = this.baseMapper.selectList(
                        new QueryWrapper<WorkOrderProcessExecution>().eq("work_order_id", workOrderId))
                .stream()
                .collect(Collectors.toMap(WorkOrderProcessExecution::getId, e -> e, (a, b) -> a));
        Set<Long> incomingIds = new HashSet<>();
        List<WorkOrderProcessExecution> toInsert = new ArrayList<>();

        if (executions != null && !executions.isEmpty()) {
            for (WorkOrderProcessExecution execution : executions) {
                execution.setWorkOrderId(workOrderId);
                execution.setIsDeleted(0);
                // 已有行：按 ID 原地更新，保留 id（按行附件可继续定位）
                if (execution.getId() != null && existingMap.containsKey(execution.getId())) {
                    incomingIds.add(execution.getId());
                    this.updateById(execution);
                } else {
                    // 新增行：清空 ID 交由自增生成
                    execution.setId(null);
                    execution.setVersion(1);
                    toInsert.add(execution);
                }
            }
            if (!toInsert.isEmpty()) {
                this.saveBatch(toInsert);
            }
        }

        // 被移除的既有行物理删除（入参中不存在的行）
        List<Long> removedIds = existingMap.keySet().stream()
                .filter(id -> !incomingIds.contains(id))
                .collect(Collectors.toList());
        if (!removedIds.isEmpty()) {
            baseMapper.physicalDeleteByIds(removedIds);
        }
    }

    // endregion
}
