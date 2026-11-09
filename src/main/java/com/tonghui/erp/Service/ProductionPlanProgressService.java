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
     * @return 采购流程详情（链路、节点、节点耗时、节点单据）
     */
    ProductionPlanPurchaseProgressDto getPurchaseProgress(Integer planId);

    /**
     * 计算生产计划逾期信息
     * <p>
     * 按自然日计算：计划生产时间以实际生产开始时间为基准，需交付时间以成品实际出库时间为基准；
     * 实际未发生时以当前时间持续累计。已完成/已归档计划保留历史逾期结果，作废计划不计算逾期。
     * </p>
     *
     * @param plan 生产计划
     * @return 逾期信息
     */
    ProductionPlanOverdueDto calculateOverdue(ProductionPlan plan);
}
