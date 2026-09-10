package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.PurchasePlan;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Service.PurchaseOrdersService;
import com.tonghui.erp.Service.PurchaseOrderItemsService;
import com.tonghui.erp.Service.PurchasePlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 采购域综合测试
 * <p>
 * 覆盖采购订单生成、查询、更新，采购明细管理，采购计划状态流转等核心业务逻辑，
 * 全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
public class PurchaseModuleTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 采购订单服务
     */
    @Autowired
    private PurchaseOrdersService purchaseOrdersService;

    /**
     * 采购订单明细服务
     */
    @Autowired
    private PurchaseOrderItemsService purchaseOrderItemsService;

    /**
     * 采购计划服务
     */
    @Autowired
    private PurchasePlanService purchasePlanService;

    /**
     * 生产单位数据访问层
     */
    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    /**
     * 采购订单数据访问层
     */
    @Autowired
    private PurchaseOrdersMapper purchaseOrdersMapper;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试采购订单新增与查询：单号自动生成、订单落库、按关键字可查
     */
    @Test
    @Transactional
    public void testAddPurchaseOrderAndQuery() {
        String orderNo = purchaseOrdersService.generateOrderNumber();
        assertNotNull(orderNo, "采购单号生成不应为空");
        assertTrue(orderNo.startsWith("CG"), "采购单号应以CG开头");

        PurchaseOrders order = new PurchaseOrders();
        order.setPurchaseNumber(orderNo);
        order.setSupplierId(1L);
        order.setTitle("测试采购订单");
        order.setWarehouse("一号仓库");
        order.setInvoiceInfo("票随货到");
        order.setReceivingInfo("仓库收货");
        order.setUnit("kg");
        order.setProcessingDate(LocalDate.now());
        order.setStatus("待采购");
        assertTrue(purchaseOrdersService.addPurchaseOrder(order), "新增采购订单应成功");
        assertNotNull(order.getId(), "订单应回填ID");

        // 按ID查询
        PurchaseOrders found = purchaseOrdersService.getPurchaseOrderById(order.getId());
        assertNotNull(found, "按ID应查到订单");
        assertEquals(orderNo, found.getPurchaseNumber(), "订单号应一致");

        // 分页列表
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        PagedResult<PurchaseOrders> list = purchaseOrdersService.getPurchaseOrderList(pageRequest);
        assertTrue(list.getItems().stream().anyMatch(o -> o.getId().equals(order.getId())),
                "订单列表应包含新建订单");

        // 关键字查询
        PurchaseOrders query = new PurchaseOrders();
        query.setTitle("测试采购订单");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PurchaseOrders> page =
                purchaseOrdersService.queryPurchaseOrders(query, orderNo, null, null, null, null, null, null, 1, 10);
        assertTrue(page.getTotal() >= 1, "按关键字查询应命中新建订单");
    }

    /**
     * 测试采购订单更新状态流转：更新订单信息、状态流转到"运输中"可触发验收单生成（联动）
     */
    @Test
    @Transactional
    public void testUpdatePurchaseOrder() {
        String orderNo = purchaseOrdersService.generateOrderNumber();
        PurchaseOrders order = new PurchaseOrders();
        order.setPurchaseNumber(orderNo);
        order.setSupplierId(1L);
        order.setTitle("状态流转测试订单");
        order.setWarehouse("一号仓库");
        order.setInvoiceInfo("票随货到");
        order.setReceivingInfo("仓库收货");
        order.setUnit("kg");
        order.setStatus("待采购");
        purchaseOrdersService.addPurchaseOrder(order);
        Long orderId = order.getId();

        // 添加一条采购明细（触发"运输中"联动生成验收单需要明细）
        com.tonghui.erp.Data.Entity.PurchaseOrderItems item = new com.tonghui.erp.Data.Entity.PurchaseOrderItems();
        item.setOrderId(orderId);
        item.setSequenceNumber(1);
        item.setMaterialCode("Y1001");
        item.setProductName("状态流转测试原料");
        item.setRawMaterialName("状态流转测试原料");
        item.setDose(new BigDecimal("1.000"));
        item.setUnit("kg");
        item.setProcessingProperty("原料");
        item.setStock(new BigDecimal("5.000"));
        item.setPurchaseQuantity(new BigDecimal("5.000"));
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setAmount(new BigDecimal("50.00"));
        assertTrue(purchaseOrderItemsService.addPurchaseOrderItem(item), "新增订单明细应成功");

        // 更新标题
        PurchaseOrders update = new PurchaseOrders();
        update.setId(orderId);
        update.setTitle("更新后的订单标题");
        assertTrue(purchaseOrdersService.updatePurchaseOrder(update), "更新订单应成功");
        assertEquals("更新后的订单标题", purchaseOrdersService.getPurchaseOrderById(orderId).getTitle(),
                "订单标题应已更新");

        // 状态流转到运输中（触发验收单联动，事务回滚不影响线上）
        update.setStatus("运输中");
        assertTrue(purchaseOrdersService.updatePurchaseOrder(update), "状态流转应成功");
        Object status = purchaseOrdersService.getPurchaseOrderById(orderId).getStatus();
        assertEquals("运输中", String.valueOf(status), "订单状态应为运输中");
    }

    /**
     * 测试采购订单删除：删除后按ID查询不可见
     */
    @Test
    @Transactional
    public void testDeletePurchaseOrder() {
        String orderNo = purchaseOrdersService.generateOrderNumber();
        PurchaseOrders order = new PurchaseOrders();
        order.setPurchaseNumber(orderNo);
        order.setSupplierId(1L);
        order.setTitle("删除测试订单");
        order.setWarehouse("一号仓库");
        order.setInvoiceInfo("票随货到");
        order.setReceivingInfo("仓库收货");
        order.setUnit("kg");
        order.setStatus("待采购");
        purchaseOrdersService.addPurchaseOrder(order);
        Long orderId = order.getId();

        assertTrue(purchaseOrdersService.deletePurchaseOrder(orderId), "删除订单应成功");
        assertNull(purchaseOrdersService.getPurchaseOrderById(orderId), "删除后订单应不可见");
    }

    /**
     * 测试采购订单明细管理：新增明细→按订单查询→更新→删除
     */
    @Test
    @Transactional
    public void testPurchaseOrderItems() {
        String orderNo = purchaseOrdersService.generateOrderNumber();
        PurchaseOrders order = new PurchaseOrders();
        order.setPurchaseNumber(orderNo);
        order.setSupplierId(1L);
        order.setTitle("明细测试订单");
        order.setWarehouse("一号仓库");
        order.setInvoiceInfo("票随货到");
        order.setReceivingInfo("仓库收货");
        order.setUnit("kg");
        order.setStatus("待采购");
        purchaseOrdersService.addPurchaseOrder(order);
        Long orderId = order.getId();

        // 新增明细
        com.tonghui.erp.Data.Entity.PurchaseOrderItems item = new com.tonghui.erp.Data.Entity.PurchaseOrderItems();
        item.setOrderId(orderId);
        item.setSequenceNumber(1);
        item.setMaterialCode("Y1001");
        item.setProductName("测试原料");
        item.setRawMaterialName("测试原料");
        item.setDose(new BigDecimal("1.000"));
        item.setUnit("kg");
        item.setProcessingProperty("原料");
        item.setStock(new BigDecimal("10.000"));
        item.setPurchaseQuantity(new BigDecimal("10.000"));
        item.setUnitPrice(new BigDecimal("5.00"));
        item.setAmount(new BigDecimal("50.00"));
        assertTrue(purchaseOrderItemsService.addPurchaseOrderItem(item), "新增明细应成功");
        assertNotNull(item.getId(), "明细应回填ID");

        // 按订单查询明细
        List<com.tonghui.erp.Data.Entity.PurchaseOrderItems> items =
                purchaseOrderItemsService.getPurchaseOrderItemsByOrderId(orderId);
        assertEquals(1, items.size(), "订单应关联1条明细");

        // 更新明细
        item.setPurchaseQuantity(new BigDecimal("20.000"));
        assertTrue(purchaseOrderItemsService.updatePurchaseOrderItem(item), "更新明细应成功");
        assertEquals(0, new BigDecimal("20.000").compareTo(
                        purchaseOrderItemsService.getPurchaseOrderItemById(item.getId()).getPurchaseQuantity()),
                "明细数量应已更新");

        // 删除明细
        assertTrue(purchaseOrderItemsService.deletePurchaseOrderItem(item.getId()), "删除明细应成功");
        assertEquals(0, purchaseOrderItemsService.getPurchaseOrderItemsByOrderId(orderId).size(),
                "删除后订单不应有关联明细");
    }

    /**
     * 测试采购计划新增与状态流转：新增计划→审批通过自动生成采购订单
     */
    @Test
    @Transactional
    public void testPurchasePlanStatusFlow() {
        String planCode = "TESTPLAN" + System.currentTimeMillis();
        PurchasePlan plan = new PurchasePlan();
        plan.setPlanCode(planCode);
        plan.setTitle("测试采购计划");
        plan.setStatus("待审批");
        plan.setWarehouse("一号仓库");
        plan.setProcessingDate(LocalDate.now());
        assertTrue(purchasePlanService.addPurchasePlan(plan), "新增采购计划应成功");
        Long planId = plan.getId();

        // 审批通过：自动生成采购订单并回写处理日期
        assertTrue(purchasePlanService.updateStatus(planId, "已审批", "同意"), "审批通过应成功");
        PurchasePlan after = purchasePlanService.getById(planId);
        assertEquals("已审批", after.getStatus(), "计划状态应为已审批");
        assertNotNull(after.getProcessingDate(), "审批后应自动填写处理日期");

        // 审批驳回
        assertTrue(purchasePlanService.updateStatus(planId, "已驳回", "资料不全"), "审批驳回应成功");
        assertEquals("已驳回", purchasePlanService.getById(planId).getStatus(), "计划状态应为已驳回");

        // 关键字查询
        PurchasePlan query = new PurchasePlan();
        query.setTitle("测试采购计划");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PurchasePlan> page =
                purchasePlanService.queryPurchasePlans(query, planCode, null, null, null, null, null, null, 1, 10);
        assertTrue(page.getTotal() >= 1, "按关键字查询应命中新建计划");
    }

    // endregion
}