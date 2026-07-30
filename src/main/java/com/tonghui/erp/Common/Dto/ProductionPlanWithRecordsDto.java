package com.tonghui.erp.Common.Dto;

import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import com.tonghui.erp.Data.Entity.WorkOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 生产计划包含工序记录和生产任务的扩展数据传输对象
 * <p>
 * 在生产计划基础上扩展了工序记录列表和生产任务列表，用于展示完整的生产计划及其关联数据
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductionPlanWithRecordsDto extends ProductionPlan {

    /**
     * 该计划的生产工序记录列表
     */
    private List<ProductionProcessRecord> records;

    /**
     * 该计划的生产任务（工单）列表
     */
    private List<WorkOrder> workOrders;
}
