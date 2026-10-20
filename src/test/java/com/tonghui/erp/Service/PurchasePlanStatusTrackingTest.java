package com.tonghui.erp.Service;

import com.tonghui.erp.Common.utils.AcceptanceStatusPolicy;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.PurchasePlan;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Data.mapper.PurchasePlanMapper;
import com.tonghui.erp.Service.impl.PurchasePlanStatusSyncer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 采购计划采购进度跟踪测试
 * <p>
 * 覆盖：采购计划审批通过自动生成采购订单后，current_status 随订单状态回写
 * （待采购 → 运输中 → 验收中 → 已入库 → 已归档），以及订单作废/归档同步到计划
 * </p>
 */
@SpringBootTest
public class PurchasePlanStatusTrackingTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 采购计划服务 */
    @Autowired
    private PurchasePlanService purchasePlanService;

    /** 采购订单服务 */
    @Autowired
    private PurchaseOrdersService purchaseOrdersService;

    /** 采购计划数据访问层 */
    @Autowired
    private PurchasePlanMapper purchasePlanMapper;

    /** 采购订单数据访问层 */
    @Autowired
    private PurchaseOrdersMapper purchaseOrdersMapper;

    /** 采购订单明细数据访问层 */
    @Autowired
    private PurchaseOrderItemsMapper purchaseOrderItemsMapper;

    /** 采购计划采购进度同步器 */
    @Autowired
    private PurchasePlanStatusSyncer purchasePlanStatusSyncer;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 审批通过初始化采购进度为"待采购"，确认采购后同步为"运输中"
     */
    @Test
    void testApprovalAndConfirmTracking() {
        PurchasePlan plan = createPlan("审批进度" + System.currentTimeMillis());
        purchasePlanService.addPurchasePlan(plan);

        // 审批通过 → 自动生成采购订单（待采购）
        purchasePlanService.updateStatus(plan.getId(), "已审批", "同意");
        PurchasePlan approved = purchasePlanMapper.selectById(plan.getId());
        assertNotNull(approved.getPurchaseOrderId(), "审批通过应生成采购订单");
        assertEquals(AcceptanceStatusPolicy.ORDER_PENDING, approved.getCurrentStatus(),
                "审批通过后采购进度应为待采购");
        assertNotNull(approved.getCurrentStatusDate(), "审批通过后应记录采购进度日期");

        PurchaseOrders order = purchaseOrdersMapper.selectById(approved.getPurchaseOrderId());
        assertEquals(AcceptanceStatusPolicy.ORDER_PENDING, String.valueOf(order.getStatus()));

        // 补充带供应商的订单明细（确认采购前置校验）
        insertOrderItem(order.getId(), 1, "审批进度测试原料");

        // 确认采购 → 订单运输中，计划同步
        purchaseOrdersService.confirmPurchase(order.getId());
        assertEquals(AcceptanceStatusPolicy.MAIN_TRANSPORTING, currentStatus(plan.getId()),
                "确认采购后采购进度应为运输中");
    }

    /**
     * 订单作废 → 计划采购进度同步为已作废
     */
    @Test
    void testVoidSyncsToPlan() {
        PurchasePlan plan = createPlanWithOrder(AcceptanceStatusPolicy.ORDER_PENDING);
        purchaseOrdersService.voidPurchase(plan.getPurchaseOrderId());
        assertEquals(AcceptanceStatusPolicy.MAIN_VOIDED, currentStatus(plan.getId()),
                "订单作废后采购进度应为已作废");
    }

    /**
     * 订单归档 → 计划采购进度同步为已归档
     */
    @Test
    void testArchiveSyncsToPlan() {
        PurchasePlan plan = createPlanWithOrder(AcceptanceStatusPolicy.MAIN_INBOUND);
        purchaseOrdersService.archivePurchase(plan.getPurchaseOrderId());
        assertEquals(AcceptanceStatusPolicy.ORDER_ARCHIVED, currentStatus(plan.getId()),
                "订单归档后采购进度应为已归档");
    }

    /**
     * 验收流程派生的订单状态（验收中/已入库）经同步器回写到计划
     */
    @Test
    void testDerivedStatusSyncsToPlan() {
        PurchasePlan plan = createPlanWithOrder(AcceptanceStatusPolicy.MAIN_TRANSPORTING);

        // 模拟验收流程回写订单状态为验收中/已入库
        PurchaseOrders order = purchaseOrdersMapper.selectById(plan.getPurchaseOrderId());
        order.setStatus(AcceptanceStatusPolicy.MAIN_INSPECTING);
        purchaseOrdersMapper.updateById(order);
        purchasePlanStatusSyncer.syncFromOrder(order.getId());
        assertEquals(AcceptanceStatusPolicy.MAIN_INSPECTING, currentStatus(plan.getId()));

        order.setStatus(AcceptanceStatusPolicy.MAIN_INBOUND);
        purchaseOrdersMapper.updateById(order);
        purchasePlanStatusSyncer.syncFromOrder(order.getId());
        assertEquals(AcceptanceStatusPolicy.MAIN_INBOUND, currentStatus(plan.getId()));
    }

    // endregion

    // region 测试辅助方法
    // ===================================
    // 测试辅助方法
    // ===================================

    /**
     * 构造采购计划（已审批，满足生成采购订单的必填字段）
     *
     * @param title 计划标题
     * @return 采购计划实体
     */
    private PurchasePlan createPlan(String title) {
        PurchasePlan plan = new PurchasePlan();
        plan.setTitle(title);
        plan.setWarehouse("原料仓");
        plan.setInvoiceInfo("测试发票信息");
        plan.setReceivingAddress("测试收货地址");
        plan.setBatchQty(BigDecimal.ONE);
        plan.setPrescriptionMultiple(BigDecimal.ONE);
        plan.setMaterialType("原料");
        plan.setStatus("已审批");
        return plan;
    }

    /**
     * 构造已关联采购订单的采购计划（用于直接测试订单状态回写计划）
     *
     * @param orderStatus 采购订单初始状态
     * @return 采购计划实体（含 purchaseOrderId）
     */
    private PurchasePlan createPlanWithOrder(String orderStatus) {
        PurchasePlan plan = createPlan("进度同步" + System.currentTimeMillis());
        purchasePlanService.addPurchasePlan(plan);

        PurchaseOrders order = new PurchaseOrders();
        order.setPurchaseNumber("TESTCGT" + System.nanoTime());
        order.setPlanId(plan.getId());
        order.setPlanCode(plan.getPlanCode());
        order.setTitle(plan.getTitle());
        order.setWarehouse(plan.getWarehouse());
        order.setInvoiceInfo(plan.getInvoiceInfo());
        order.setReceivingInfo(plan.getReceivingAddress());
        order.setUnit("测试单位");
        order.setPrescriptionMultiple(BigDecimal.ONE);
        order.setStatus(orderStatus);
        purchaseOrdersMapper.insert(order);

        plan.setPurchaseOrderId(order.getId());
        plan.setCurrentStatus(orderStatus);
        plan.setCurrentStatusDate(LocalDateTime.now());
        purchasePlanMapper.updateById(plan);
        return plan;
    }

    /**
     * 插入一条采购订单明细（含供应商，满足确认采购前置校验）
     *
     * @param orderId 采购订单ID
     * @param seq     序号
     * @param name    原料名称
     */
    private void insertOrderItem(Long orderId, int seq, String name) {
        PurchaseOrderItems item = new PurchaseOrderItems();
        item.setOrderId(orderId);
        item.setSequenceNumber(seq);
        item.setProductName("测试品名");
        item.setRawMaterialName(name);
        item.setDose(BigDecimal.ONE);
        item.setUnit("kg");
        item.setProcessingProperty("原料");
        item.setStock(BigDecimal.ZERO);
        item.setPurchaseQuantity(BigDecimal.ONE);
        item.setSupplier("测试供应商");
        purchaseOrderItemsMapper.insert(item);
    }

    /**
     * 查询计划当前采购进度
     *
     * @param planId 采购计划ID
     * @return 采购进度
     */
    private String currentStatus(Long planId) {
        return purchasePlanMapper.selectById(planId).getCurrentStatus();
    }

    // endregion
}
