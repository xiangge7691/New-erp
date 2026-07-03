package com.tonghui.erp.Common.Dto;

import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 生产计划包含工序记录的扩展数据传输对象
 * <p>
 * 在生产计划基础上扩展了工序记录列表，用于展示完整的生产计划及其关联的生产工序执行记录
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductionPlanWithRecordsDto extends ProductionPlan {

    /**
     * 该计划的生产工序记录列表
     */
    private List<ProductionProcessRecord> records;
}
