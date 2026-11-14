package com.tonghui.erp.Service.businesshistory;

import com.tonghui.erp.Common.Constants.BizFileType;
import com.tonghui.erp.Common.Dto.Log.BusinessHistoryScope;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 生产任务（工单）变更历史 Provider
 * <p>
 * 工单历史包含主表 work_order 与工序执行 work_order_process_execution；
 * 工序执行的变更日志以执行记录 ID 作为 business_id，工序附件变更从
 * file_operation_log 聚合后归到对应工序行
 * </p>
 */
@Component
public class WorkOrderHistoryProvider implements BusinessHistoryProvider {

    // region 依赖
    // ===================================
    // 依赖
    // ===================================

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderProcessExecutionMapper processExecutionMapper;

    /**
     * 构造生产任务变更历史 Provider
     *
     * @param workOrderMapper         工单Mapper
     * @param processExecutionMapper  工单工序执行Mapper
     */
    public WorkOrderHistoryProvider(WorkOrderMapper workOrderMapper,
                                    WorkOrderProcessExecutionMapper processExecutionMapper) {
        this.workOrderMapper = workOrderMapper;
        this.processExecutionMapper = processExecutionMapper;
    }

    // endregion

    // region Provider 实现
    // ===================================
    // Provider 实现
    // ===================================

    /**
     * 业务类型
     *
     * @return WORK_ORDER
     */
    @Override
    public String bizType() {
        return "WORK_ORDER";
    }

    /**
     * 解析工单的查询范围
     *
     * @param bizId 工单ID
     * @return 查询范围
     */
    @Override
    public BusinessHistoryScope resolve(String bizId) {
        Long id = parseId(bizId);
        WorkOrder workOrder = workOrderMapper.selectById(id);
        List<WorkOrderProcessExecution> executions = processExecutionMapper.selectByWorkOrderIdIncludeDeleted(id);

        BusinessHistoryScope scope = new BusinessHistoryScope();
        scope.setTitle(buildTitle(id, workOrder));
        scope.addTable("work_order", List.of(String.valueOf(id)));
        scope.addFileScope(BizFileType.WORK_ORDER.getCode(), String.valueOf(id), "work_order");

        if (executions != null && !executions.isEmpty()) {
            List<String> executionIds = new ArrayList<>(executions.size());
            for (WorkOrderProcessExecution execution : executions) {
                if (execution.getId() == null) {
                    continue;
                }
                String executionId = String.valueOf(execution.getId());
                executionIds.add(executionId);
                scope.addRecordLabel("work_order_process_execution", executionId, processLabel(execution));
            }
            scope.addTable("work_order_process_execution", executionIds);
            scope.addFileScope(BizFileType.WORK_ORDER_PROCESS_EXECUTION.getCode(), executionIds,
                    "work_order_process_execution");
        }
        return scope;
    }

    // endregion

    // region 辅助方法
    // ===================================
    // 辅助方法
    // ===================================

    /**
     * 解析业务ID
     *
     * @param bizId 业务ID字符串
     * @return 业务ID
     */
    private Long parseId(String bizId) {
        try {
            return Long.valueOf(bizId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("业务ID不合法：" + bizId);
        }
    }

    /**
     * 构建工序展示名称
     *
     * @param execution 工单工序执行记录
     * @return 工序名称
     */
    private String processLabel(WorkOrderProcessExecution execution) {
        String name = execution.getProcessTypeName();
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return "工序 #" + execution.getId();
    }

    /**
     * 构建业务对象展示名称
     *
     * @param id        工单ID
     * @param workOrder 工单
     * @return 展示名称
     */
    private String buildTitle(Long id, WorkOrder workOrder) {
        if (workOrder == null) {
            return "工单 #" + id;
        }
        String code = workOrder.getWorkOrderCode() == null ? "" : workOrder.getWorkOrderCode();
        String name = workOrder.getWorkOrderName() == null ? "" : workOrder.getWorkOrderName();
        if (code.isEmpty()) {
            return name.isEmpty() ? "工单 #" + id : name;
        }
        return name.isEmpty() ? code : code + " · " + name;
    }

    // endregion
}
