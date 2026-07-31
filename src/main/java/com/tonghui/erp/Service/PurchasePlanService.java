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
     * 提交审批
     *
     * @param planId 采购计划ID
     * @return 是否成功
     */
    boolean submitForApproval(Long planId);

    /**
     * 审批通过（自动生成采购订单）
     *
     * @param planId         采购计划ID
     * @param approvalOpinion 审批意见
     * @return 是否成功
     */
    boolean approve(Long planId, String approvalOpinion);

    /**
     * 驳回
     *
     * @param planId         采购计划ID
     * @param approvalOpinion 驳回原因
     * @return 是否成功
     */
    boolean reject(Long planId, String approvalOpinion);

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
