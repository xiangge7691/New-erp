package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.ProductionPlanOverdueDto;
import com.tonghui.erp.Common.Dto.ProductionPlanPurchaseProgressDto;
import com.tonghui.erp.Common.Dto.ProductionPlanWithRecordsDto;
import com.tonghui.erp.Data.Entity.ProductionPlan;

import java.util.List;

/**
 * 生产计划采购进度服务接口
 * <p>
 * 提供生产计划关联采购流程的摘要填充和详情时间线查询能力。
 * 采购流程节点时间来自采购计划、采购订单、验收单、入库单上的业务时间字段，
 * 不新增流程事件表。
 * </p>
 */
public interface ProductionPlanProgressService {

    /**
     * 批量为生产计划列表填充采购进度摘要和逾期信息
     *
     * @param plans 生产计划列表（含工序记录、工单的 DTO）
     */
    void fillPurchaseSummaries(List<ProductionPlanWithRecordsDto> plans);

    /**
     * 查询生产计划采购流程详情
     *
     * @param planId 生产计划ID
     * @return 采购流程详情（链路、节点、耗时、关联单据）
     */
    ProductionPlanPurchaseProgressDto getPurchaseProgress(Integer planId);

    /**
     * 计算生产计划逾期信息
     *
     * @param plan 生产计划
     * @return 逾期信息
     */
    ProductionPlanOverdueDto calculateOverdue(ProductionPlan plan);
}
