package com.tonghui.erp.Service.businesshistory;

import com.tonghui.erp.Common.Dto.Log.BusinessHistoryScope;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 生产任务（工单）变更历史 Provider
 * <p>
 * 工单的字段变更全部记录在 work_order 表，直接按主键查询即可
 * </p>
 */
@Component
public class WorkOrderHistoryProvider implements BusinessHistoryProvider {

    // region 依赖
    // ===================================
    // 依赖
    // ===================================

    private final WorkOrderMapper workOrderMapper;

    /**
     * 构造生产任务变更历史 Provider
     *
     * @param workOrderMapper 工单Mapper
     */
    public WorkOrderHistoryProvider(WorkOrderMapper workOrderMapper) {
        this.workOrderMapper = workOrderMapper;
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

        BusinessHistoryScope scope = new BusinessHistoryScope();
        scope.setTitle(buildTitle(id, workOrder));
        scope.addTable("work_order", List.of(String.valueOf(id)));
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
