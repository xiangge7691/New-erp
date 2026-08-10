package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.PurchasePlan;

import java.time.LocalDate;
import java.util.List;

/**
 * 采购计划服务接口
 */
public interface PurchasePlanService extends IService<PurchasePlan> {

    /**
     * 创建采购计划（含明细）
     *
     * @param purchasePlan 采购计划实体
     * @return 是否成功
     */
    boolean addPurchasePlan(PurchasePlan purchasePlan);

    /**
     * 更新采购计划状态（通用状态变更接口）
     * <p>
     * 前端控制按钮显隐和状态流转逻辑，后端只负责更新状态。
     * 当目标状态为"已审批"时，自动触发生成采购订单的逻辑。
     * </p>
     *
     * @param planId         采购计划ID
     * @param targetStatus   目标状态
     * @param approvalOpinion 审批意见（可选）
     * @return 是否成功
     */
    boolean updateStatus(Long planId, String targetStatus, String approvalOpinion);

    /**
     * 高级查询采购计划（支持多条件组合查询）
     * <p>支持按计划编号、生产计划、标题、制剂、物料类型、仓库、状态等条件筛选，
     * 也支持处理日期、期望到货日期、预计到货日期等时间范围筛选</p>
     *
     * @param purchasePlan            查询条件实体，非null字段将作为等值或模糊查询条件
     * @param keyword                 关键字（对计划编号、生产计划编号、标题、制剂名称进行模糊匹配，可选）
     * @param processingDateStart     处理日期起始（可选）
     * @param processingDateEnd       处理日期结束（可选）
     * @param desiredDeliveryDateStart 期望到货日期起始（可选）
     * @param desiredDeliveryDateEnd   期望到货日期结束（可选）
     * @param expectedDeliveryDateStart 预计到货日期起始（可选）
     * @param expectedDeliveryDateEnd   预计到货日期结束（可选）
     * @param pageIndex               页码（从0开始）
     * @param pageSize                每页数量
     * @return 分页结果
     */
    Page<PurchasePlan> queryPurchasePlans(PurchasePlan purchasePlan, String keyword,
                                          LocalDate processingDateStart, LocalDate processingDateEnd,
                                          LocalDate desiredDeliveryDateStart, LocalDate desiredDeliveryDateEnd,
                                          LocalDate expectedDeliveryDateStart, LocalDate expectedDeliveryDateEnd,
                                          int pageIndex, int pageSize);
}
