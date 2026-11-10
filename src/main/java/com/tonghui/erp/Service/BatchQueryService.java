package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.QualityPages.BatchContext;
import com.tonghui.erp.Data.Entity.ProductionPlan;
import com.tonghui.erp.Data.Entity.WorkOrder;

import java.util.List;

/**
 * 批次聚合查询服务
 * <p>
 * 一次加载一个工单/批次在采购、验收、领料、退库、出入库、检验、放行、
 * 偏差、工序等链路上的相关数据，供产品信息汇总和审核放行共用。
 * </p>
 */
public interface BatchQueryService {

    /**
     * 加载单个批次上下文
     *
     * @param workOrder 工单
     * @return 批次上下文
     */
    BatchContext load(WorkOrder workOrder);

    /**
     * 批量加载批次上下文
     *
     * @param workOrders 工单列表
     * @return 批次上下文列表
     */
    List<BatchContext> loadAll(List<WorkOrder> workOrders);

    /**
     * 加载未下达工单的生产计划上下文
     * <p>
     * 仅加载计划、采购、验收和物料入库数据，用于产品信息汇总中未下达工单的记录。
     * </p>
     *
     * @param plan 生产计划
     * @return 计划上下文
     */
    BatchContext loadByPlan(ProductionPlan plan);
}
