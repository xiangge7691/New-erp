package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Data.Entity.PurchasePlan;

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
     * 高级查询采购计划
     *
     * @param status    状态（可选）
     * @param keyword   关键字（可选）
     * @param pageIndex 页码
     * @param pageSize  每页数量
     * @return 分页结果
     */
    Page<PurchasePlan> queryPurchasePlans(String status, String keyword, int pageIndex, int pageSize);
}
