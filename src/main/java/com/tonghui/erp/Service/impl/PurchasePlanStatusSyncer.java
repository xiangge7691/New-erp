package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.PurchasePlan;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Data.mapper.PurchasePlanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购计划采购进度同步器
 * <p>
 * 采购计划审批通过后会自动生成采购订单，计划侧需要通过 current_status 跟踪审批后的采购进度。
 * 本组件按采购订单状态变化回写关联采购计划的采购进度（复用采购订单状态词表：
 * 待采购/运输中/验收中/已入库/已结束/已作废/已归档）。
 * </p>
 * <p>
 * 仅依赖 Mapper：采购计划服务本身依赖采购订单服务，若订单/验收侧再反向依赖采购计划服务会形成循环依赖，
 * 因此独立为只依赖 Mapper 的同步组件。
 * </p>
 */
@Component
public class PurchasePlanStatusSyncer {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 采购计划数据访问层 */
    @Autowired
    private PurchasePlanMapper purchasePlanMapper;

    /** 采购订单数据访问层 */
    @Autowired
    private PurchaseOrdersMapper purchaseOrdersMapper;

    // endregion

    // region 同步方法
    // ===================================
    // 同步方法
    // ===================================

    /**
     * 按采购订单ID同步关联采购计划的采购进度
     * <p>
     * 订单不存在（含已删除）或未关联采购计划时静默跳过；进度与订单状态一致时不重复刷新日期。
     * </p>
     *
     * @param orderId 采购订单ID
     */
    public void syncFromOrder(Long orderId) {
        if (orderId == null) {
            return;
        }
        PurchaseOrders order = purchaseOrdersMapper.selectById(orderId);
        if (order == null || order.getStatus() == null) {
            return;
        }
        String status = String.valueOf(order.getStatus());
        if (!StringUtils.hasText(status)) {
            return;
        }

        // 按采购订单ID定位关联采购计划（计划与订单为 1:1）
        QueryWrapper<PurchasePlan> wrapper = new QueryWrapper<>();
        wrapper.eq("purchase_order_id", orderId);
        wrapper.eq("is_deleted", 0);
        List<PurchasePlan> plans = purchasePlanMapper.selectList(wrapper);
        if (plans.isEmpty()) {
            return;
        }

        PurchasePlan plan = plans.get(0);
        // 状态未变化时保留原更新日期
        if (status.equals(plan.getCurrentStatus())) {
            return;
        }
        plan.setCurrentStatus(status);
        plan.setCurrentStatusDate(LocalDateTime.now());
        purchasePlanMapper.updateById(plan);
    }

    // endregion
}
