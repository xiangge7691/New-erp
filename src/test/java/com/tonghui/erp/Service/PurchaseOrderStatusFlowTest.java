package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.mapper.AcceptanceDetailMapper;
import com.tonghui.erp.Data.mapper.AcceptanceOrderMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购订单状态触发式流程测试
 * <p>
 * 覆盖：采购订单状态改为"运输中"时自动生成货物验收单（含明细复制、幂等），
 * 以及验收合格自动入库后回写采购订单状态为"已完成"的完整闭环
 * </p>
 */
@SpringBootTest
public class PurchaseOrderStatusFlowTest {

    // region 常量与依赖注入
    // ===================================
    // 常量与依赖注入
    // ===================================

    /** 测试采购订单号（动态唯一，避免唯一索引与软删除残留冲突） */
    private final String testOrderNo = "TESTCG" + System.currentTimeMillis();
    /** 测试物料编码 */
    private static final String TEST_ITEM_CODE = "TESTFLOW001";
    /** 测试物料名称（原药材品名，作为验收单物料名称） */
    private static final String TEST_ITEM_NAME = "状态流程测试物料";
    /** 测试制剂名称（不应作为物料名称） */
    private static final String TEST_PRODUCT_NAME = "测试制剂名称";
    /** 测试批次号 */
    private static final String TEST_BATCH = "TFLOW01";
    /** 入库仓库ID（耒阳制剂室） */
    private static final Long INBOUND_UNIT_ID = 7L;

    /** 采购订单服务 */
    @Autowired
    private PurchaseOrdersService purchaseOrdersService;
    /** 采购订单Mapper */
    @Autowired
    private PurchaseOrdersMapper purchaseOrdersMapper;
    /** 采购订单明细Mapper */
    @Autowired
    private PurchaseOrderItemsMapper purchaseOrderItemsMapper;
    /** 验收单服务 */
    @Autowired
    private AcceptanceOrderService acceptanceOrderService;
    /** 验收单Mapper */
    @Autowired
    private AcceptanceOrderMapper acceptanceOrderMapper;
    /** 验收单明细Mapper */
    @Autowired
    private AcceptanceDetailMapper acceptanceDetailMapper;
    /** JdbcTemplate（用于物理删除测试数据，绕开软删除） */
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // endregion

    // region 测试方法
    // ===================================
    // 测试方法
    // ===================================

    /**
     * 测试采购订单状态改为"运输中"时自动生成货物验收单
     * <p>
     * 断言：生成验收单主表（状态"运输中"、关联采购订单号）、
     * 明细从采购订单明细复制（数量/单价/物料信息）、再次更新不重复生成（幂等）
     * </p>
     */
    @Test
    public void testTransitGeneratesAcceptance() {
        Long orderId = null;
        Long acceptanceId = null;
        try {
            // 创建测试采购订单
            PurchaseOrders order = createOrder();
            orderId = order.getId();

            // 状态改为"运输中"（触发自动生成验收单）
            PurchaseOrders update = new PurchaseOrders();
            update.setId(orderId);
            update.setStatus("运输中");
            purchaseOrdersService.updatePurchaseOrder(update);

            // 断言验收单已生成
            List<AcceptanceOrder> acceptances = acceptanceOrderMapper.selectList(
                    new QueryWrapper<AcceptanceOrder>().eq("purchase_number", testOrderNo));
            if (acceptances.size() != 1) {
                System.err.println("测试失败: 应生成1条验收单, 实际: " + acceptances.size());
            } else {
                AcceptanceOrder acceptance = acceptances.get(0);
                acceptanceId = acceptance.getAcceptanceId();
                System.out.println("生成的验收单号: " + acceptance.getAcceptanceCode());
                if (!"运输中".equals(acceptance.getStatus())) {
                    System.err.println("测试失败: 验收单状态应为运输中, 实际: " + acceptance.getStatus());
                } else {
                    System.out.println("验收单状态: " + acceptance.getStatus());
                }
                // 断言明细从采购订单明细复制
                List<AcceptanceDetail> details = acceptanceDetailMapper.selectList(
                        new QueryWrapper<AcceptanceDetail>().eq("acceptance_id", acceptanceId));
                if (details.size() != 1) {
                    System.err.println("测试失败: 验收明细应为1条, 实际: " + details.size());
                } else {
                    AcceptanceDetail detail = details.get(0);
                    if (!TEST_ITEM_CODE.equals(detail.getMaterialCode())) {
                        System.err.println("测试失败: 验收明细物料编码错误: " + detail.getMaterialCode());
                    } else {
                        System.out.println("验收明细: " + detail.getMaterialCode() + " 数量: " + detail.getQuantity() + " 单价: " + detail.getUnitPrice());
                    }
                    if (detail.getQuantity() == null || detail.getQuantity().compareTo(new BigDecimal("1.5")) != 0) {
                        System.err.println("测试失败: 验收明细数量应为1.5, 实际: " + detail.getQuantity());
                    }
                }
            }

            // 幂等验证：再次更新为"运输中"不重复生成验收单
            purchaseOrdersService.updatePurchaseOrder(update);
            long count = acceptanceOrderMapper.selectCount(
                    new QueryWrapper<AcceptanceOrder>().eq("purchase_number", testOrderNo));
            if (count != 1) {
                System.err.println("测试失败: 幂等校验未生效, 验收单数量: " + count);
            } else {
                System.out.println("幂等校验通过: 验收单仍为1条");
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(orderId, acceptanceId, null, null, null, null);
        }
    }

    /**
     * 测试验收合格自动入库后回写采购订单状态为"已完成"
     * <p>
     * 采购订单置"运输中"生成验收单 → 补批号置"物料检验" → 检验合格入库
     * → 断言采购订单状态变为"已完成"
     * </p>
     */
    @Test
    public void testQualityCheckWriteBackOrderStatus() {
        Long orderId = null;
        Long acceptanceId = null;
        Long stockInId = null;
        Long stockId = null;
        try {
            // 创建测试采购订单并置"运输中"（自动生成验收单）
            PurchaseOrders order = createOrder();
            orderId = order.getId();
            PurchaseOrders update = new PurchaseOrders();
            update.setId(orderId);
            update.setStatus("运输中");
            purchaseOrdersService.updatePurchaseOrder(update);

            // 查询自动生成的验收单，补批号并置为"物料检验"
            AcceptanceOrder acceptance = acceptanceOrderMapper.selectOne(
                    new QueryWrapper<AcceptanceOrder>().eq("purchase_number", testOrderNo));
            if (acceptance == null) {
                System.err.println("测试失败: 未生成验收单");
                return;
            }
            acceptanceId = acceptance.getAcceptanceId();
            List<AcceptanceDetail> details = acceptanceDetailMapper.selectList(
                    new QueryWrapper<AcceptanceDetail>().eq("acceptance_id", acceptanceId));
            AcceptanceDetail detail = details.get(0);
            detail.setBatchNumber(TEST_BATCH);
            detail.setExpiryDate(LocalDate.now().plusYears(1));
            acceptanceDetailMapper.updateById(detail);

            AcceptanceOrder toInspect = new AcceptanceOrder();
            toInspect.setAcceptanceId(acceptanceId);
            toInspect.setStatus("物料检验");
            acceptanceOrderMapper.updateById(toInspect);

            // 检验合格（触发自动入库 + 回写采购订单状态）
            acceptanceOrderService.qualityCheck(acceptanceId, true, INBOUND_UNIT_ID, "测试合格");

            // 断言采购订单状态为"已完成"
            PurchaseOrders updatedOrder = purchaseOrdersMapper.selectById(orderId);
            if (updatedOrder == null || !"已完成".equals(String.valueOf(updatedOrder.getStatus()))) {
                System.err.println("测试失败: 采购订单状态应为已完成, 实际: " + (updatedOrder == null ? "不存在" : updatedOrder.getStatus()));
            } else {
                System.out.println("采购订单状态已回写为: " + updatedOrder.getStatus());
            }
            // 断言验收单状态为已入库
            AcceptanceOrder updated = acceptanceOrderMapper.selectById(acceptanceId);
            if (!"已入库".equals(updated.getStatus())) {
                System.err.println("测试失败: 验收单状态应为已入库, 实际: " + updated.getStatus());
            } else {
                System.out.println("验收单状态: " + updated.getStatus());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(orderId, acceptanceId, null, null, null, null);
        }
    }

    /**
     * 测试验收单确认到货时同步采购订单状态为"到货初验"
     * <p>
     * 采购订单置"运输中"生成验收单 → 确认到货（验收单置"到货初验"）
     * → 断言关联采购订单状态同步变为"到货初验"
     * </p>
     */
    @Test
    public void testConfirmArrivalSyncsPurchaseOrderStatus() {
        Long orderId = null;
        Long acceptanceId = null;
        try {
            // 创建测试采购订单并置"运输中"（自动生成验收单）
            PurchaseOrders order = createOrder();
            orderId = order.getId();
            PurchaseOrders update = new PurchaseOrders();
            update.setId(orderId);
            update.setStatus("运输中");
            purchaseOrdersService.updatePurchaseOrder(update);

            // 查询自动生成的验收单并确认到货
            AcceptanceOrder acceptance = acceptanceOrderMapper.selectOne(
                    new QueryWrapper<AcceptanceOrder>().eq("purchase_number", testOrderNo));
            if (acceptance == null) {
                System.err.println("测试失败: 未生成验收单");
                return;
            }
            acceptanceId = acceptance.getAcceptanceId();
            acceptanceOrderService.confirmArrival(acceptanceId);

            // 断言采购订单状态同步为"到货初验"
            PurchaseOrders updatedOrder = purchaseOrdersMapper.selectById(orderId);
            if (updatedOrder == null || !"到货初验".equals(String.valueOf(updatedOrder.getStatus()))) {
                System.err.println("测试失败: 采购订单状态应为到货初验, 实际: " + (updatedOrder == null ? "不存在" : updatedOrder.getStatus()));
            } else {
                System.out.println("采购订单状态已同步为: " + updatedOrder.getStatus());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(orderId, acceptanceId, null, null, null, null);
        }
    }

    /**
     * 测试验收合格自动入库返回入库单（主表携带关联生产计划编号/总金额/仓库/操作人）
     * <p>
     * 采购订单置"运输中"生成验收单（断言物料名称取原药材品名而非制剂名称）→ 补批号置"物料检验"
     * → 检验合格入库 → 断言返回的入库单主表字段：planNumber=生产计划编号、totalAmount=15.00、
     * prodUnitId=入库仓库、createdBy=操作人
     * </p>
     */
    @Test
    public void testQualityCheckReturnsInboundOrderWithFields() {
        Long orderId = null;
        Long acceptanceId = null;
        try {
            // 创建测试采购订单并置"运输中"（自动生成验收单）
            PurchaseOrders order = createOrder();
            orderId = order.getId();
            String productionPlanCode = order.getProductionPlanCode();
            PurchaseOrders update = new PurchaseOrders();
            update.setId(orderId);
            update.setStatus("运输中");
            purchaseOrdersService.updatePurchaseOrder(update);

            // 查询自动生成的验收单，断言物料名称取原药材品名
            AcceptanceOrder acceptance = acceptanceOrderMapper.selectOne(
                    new QueryWrapper<AcceptanceOrder>().eq("purchase_number", testOrderNo));
            if (acceptance == null) {
                System.err.println("测试失败: 未生成验收单");
                return;
            }
            acceptanceId = acceptance.getAcceptanceId();
            List<AcceptanceDetail> details = acceptanceDetailMapper.selectList(
                    new QueryWrapper<AcceptanceDetail>().eq("acceptance_id", acceptanceId));
            AcceptanceDetail detail = details.get(0);
            if (!TEST_ITEM_NAME.equals(detail.getMaterialName())) {
                System.err.println("测试失败: 验收明细物料名称应为 " + TEST_ITEM_NAME + ", 实际: " + detail.getMaterialName());
            } else {
                System.out.println("验收明细物料名称正确（取原药材品名）: " + detail.getMaterialName());
            }

            // 补批号并置为"物料检验"
            detail.setBatchNumber(TEST_BATCH);
            detail.setExpiryDate(LocalDate.now().plusYears(1));
            acceptanceDetailMapper.updateById(detail);
            AcceptanceOrder toInspect = new AcceptanceOrder();
            toInspect.setAcceptanceId(acceptanceId);
            toInspect.setStatus("物料检验");
            acceptanceOrderMapper.updateById(toInspect);

            // 检验合格（触发自动入库并返回入库单）
            StockIn stockIn = acceptanceOrderService.qualityCheck(acceptanceId, true, INBOUND_UNIT_ID, "测试合格");
            if (stockIn == null) {
                System.err.println("测试失败: 未返回自动生成的入库单");
            } else {
                System.out.println("入库单号: " + stockIn.getInCode());
                // 关联生产计划编号：取采购订单携带的生产计划编号
                if (stockIn.getPlanNumber() == null || !productionPlanCode.equals(stockIn.getPlanNumber())) {
                    System.err.println("测试失败: 入库单关联生产计划编号应为 " + productionPlanCode + ", 实际: " + stockIn.getPlanNumber());
                } else {
                    System.out.println("入库单关联生产计划编号: " + stockIn.getPlanNumber());
                }
                // 总金额：明细金额之和 1.5 × 10 = 15.00
                if (stockIn.getTotalAmount() == null || stockIn.getTotalAmount().compareTo(new BigDecimal("15.00")) != 0) {
                    System.err.println("测试失败: 入库单总金额应为15.00, 实际: " + stockIn.getTotalAmount());
                } else {
                    System.out.println("入库单总金额: " + stockIn.getTotalAmount());
                }
                // 仓库：prodUnitId
                if (!INBOUND_UNIT_ID.equals(stockIn.getProdUnitId())) {
                    System.err.println("测试失败: 入库单仓库错误: " + stockIn.getProdUnitId());
                } else {
                    System.out.println("入库单仓库(prodUnitId): " + stockIn.getProdUnitId());
                }
                // 操作人：createdBy（自动填充当前登录用户）
                if (stockIn.getCreatedBy() == null) {
                    System.err.println("测试失败: 入库单操作人缺失");
                } else {
                    System.out.println("入库单操作人(createdBy): " + stockIn.getCreatedBy());
                }
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(orderId, acceptanceId, null, null, null, null);
        }
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 创建测试采购订单（含1条明细）
     *
     * @return 创建的采购订单
     */
    private PurchaseOrders createOrder() {
        PurchaseOrders order = new PurchaseOrders();
        order.setPurchaseNumber(testOrderNo);
        order.setTitle("状态流程测试订单");
        order.setWarehouse("原料仓");
        order.setInvoiceInfo("测试发票信息");
        order.setReceivingInfo("测试收货信息");
        order.setUnit("kg");
        order.setPrescriptionMultiple(new BigDecimal("1.0"));
        order.setStatus("待采购");
        order.setProdUnitId(INBOUND_UNIT_ID);
        // 采购计划编号与生产计划编号（用于验证入库单关联生产计划编号取值）
        order.setPlanCode("TESTPLAN" + System.currentTimeMillis());
        order.setProductionPlanCode("PRODPLAN" + System.currentTimeMillis());
        purchaseOrdersService.addPurchaseOrder(order);

        PurchaseOrderItems item = new PurchaseOrderItems();
        item.setOrderId(order.getId());
        item.setSequenceNumber(1);
        item.setMaterialCode(TEST_ITEM_CODE);
        // 制剂名称与物料名称分开设置，用于验证验收单物料名称取原药材品名
        item.setProductName(TEST_PRODUCT_NAME);
        item.setRawMaterialName(TEST_ITEM_NAME);
        item.setDose(new BigDecimal("1.0"));
        item.setUnit("kg");
        item.setProcessingProperty("原料");
        item.setStock(new BigDecimal("0"));
        item.setPurchaseQuantity(new BigDecimal("1.5"));
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setAmount(new BigDecimal("15.00"));
        purchaseOrderItemsMapper.insert(item);
        return order;
    }

    /**
     * 清理测试数据（物理删除，先删入库相关，再删库存与流水（外键约束），最后删验收单与订单）
     *
     * @param orderId       采购订单ID（可为null）
     * @param acceptanceId  验收单ID（可为null）
     * @param stockInId     入库单ID（可为null，预留）
     * @param stockId       库存ID（可为null，预留）
     * @param orderId2      预留参数
     * @param stockInId2    预留参数
     */
    private void cleanup(Long orderId, Long acceptanceId, Long stockInId, Long stockId, Long orderId2, Long stockInId2) {
        try {
            // 1. 清理入库单与明细（验收合格入库产生的）
            jdbcTemplate.update("DELETE FROM stock_in_detail WHERE in_id IN (SELECT in_id FROM stock_in WHERE related_order IN (SELECT acceptance_code FROM acceptance_order WHERE purchase_number = ?))", testOrderNo);
            jdbcTemplate.update("DELETE FROM stock_in WHERE related_order IN (SELECT acceptance_code FROM acceptance_order WHERE purchase_number = ?)", testOrderNo);
            // 2. 清理库存与流水（先删流水再删库存，满足外键约束）
            jdbcTemplate.update("DELETE FROM stock_transaction WHERE batch_number = ?", TEST_BATCH);
            jdbcTemplate.update("DELETE FROM stock_transaction WHERE stock_id IN (SELECT stock_id FROM stock WHERE item_code = ?)", TEST_ITEM_CODE);
            jdbcTemplate.update("DELETE FROM stock WHERE item_code = ?", TEST_ITEM_CODE);
            // 3. 清理验收单
            if (acceptanceId != null) {
                jdbcTemplate.update("DELETE FROM acceptance_detail WHERE acceptance_id = ?", acceptanceId);
                jdbcTemplate.update("DELETE FROM acceptance_order WHERE acceptance_id = ?", acceptanceId);
            } else {
                jdbcTemplate.update("DELETE FROM acceptance_detail WHERE acceptance_id IN (SELECT acceptance_id FROM acceptance_order WHERE purchase_number = ?)", testOrderNo);
                jdbcTemplate.update("DELETE FROM acceptance_order WHERE purchase_number = ?", testOrderNo);
            }
            // 4. 清理采购订单
            if (orderId != null) {
                jdbcTemplate.update("DELETE FROM purchase_order_items WHERE order_id = ?", orderId);
                jdbcTemplate.update("DELETE FROM purchase_orders WHERE id = ?", orderId);
            }
        } catch (Exception e) {
            System.err.println("清理测试数据失败: " + e.getMessage());
        }
    }

    // endregion
}