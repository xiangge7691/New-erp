package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Data.Entity.AcceptanceDetail;
import com.tonghui.erp.Data.Entity.AcceptanceOrder;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.PurchaseOrderItems;
import com.tonghui.erp.Data.Entity.PurchaseOrders;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.mapper.AcceptanceDetailMapper;
import com.tonghui.erp.Data.mapper.AcceptanceOrderMapper;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrderItemsMapper;
import com.tonghui.erp.Data.mapper.PurchaseOrdersMapper;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Stock.AcceptanceWithDetailsDto;
import com.tonghui.erp.Common.Dto.Stock.StockInWithDetailsDto;
import com.tonghui.erp.Common.Dto.Stock.StockInWithNamesDto;
import com.tonghui.erp.Common.Dto.Stock.StockTransactionDto;
import com.tonghui.erp.Service.StockInService;
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
 * 以及验收单全流程状态变更（确认到货/初验/检验/重新收货）时按同名映射同步采购订单状态，
 * 含验收合格自动入库的完整闭环
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
    /** 测试批次号 */
    private static final String TEST_BATCH = "TFLOW01";
    /** 陈旧的原药材品名（验证物料主数据优先级） */
    private static final String STALE_RAW_NAME = "陈旧物料名";
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
    /** 物料Mapper（创建物料主数据） */
    @Autowired
    private MaterialMapper materialMapper;
    /** 验收单服务 */
    @Autowired
    private AcceptanceOrderService acceptanceOrderService;
    /** 验收单Mapper */
    @Autowired
    private AcceptanceOrderMapper acceptanceOrderMapper;
    /** 验收单明细Mapper */
    @Autowired
    private AcceptanceDetailMapper acceptanceDetailMapper;
    /** 库存服务（验证库存流水绑定入库单与验收单） */
    @Autowired
    private StockService stockService;
    /** 入库单服务（验证入库单查询回填操作人姓名/仓库名称） */
    @Autowired
    private StockInService stockInService;
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
 * 测试验收合格自动入库后回写采购订单状态为"已入库"
 * <p>
 * 采购订单置"运输中"生成验收单 → 补批号置"物料检验" → 检验合格入库
 * → 断言采购订单状态变为"已入库"（与验收单状态一致）
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

            // 断言采购订单状态为"已入库"
            PurchaseOrders updatedOrder = purchaseOrdersMapper.selectById(orderId);
            if (updatedOrder == null || !"已入库".equals(String.valueOf(updatedOrder.getStatus()))) {
                System.err.println("测试失败: 采购订单状态应为已入库, 实际: " + (updatedOrder == null ? "不存在" : updatedOrder.getStatus()));
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

            // 查询自动生成的验收单，断言物料名称取物料主数据（非制剂名称、非陈旧原药材品名）
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
                System.err.println("测试失败: 验收明细物料名称应为物料主数据 " + TEST_ITEM_NAME + ", 实际: " + detail.getMaterialName());
            } else {
                System.out.println("验收明细物料名称正确（取物料主数据）: " + detail.getMaterialName());
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
            StockInWithNamesDto stockIn = acceptanceOrderService.qualityCheck(acceptanceId, true, INBOUND_UNIT_ID, "测试合格");
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
                // 仓库名称：应解析为生产单位名称（测试环境 prodUnitId=7 → 耒阳制剂室）
                if (!"耒阳制剂室".equals(stockIn.getWarehouseName())) {
                    System.err.println("测试失败: 入库单仓库名称应为耒阳制剂室, 实际: " + stockIn.getWarehouseName());
                } else {
                    System.out.println("入库单仓库名称: " + stockIn.getWarehouseName());
                }
                // 操作人姓名：应解析为用户姓名（无登录态默认用户1 → 超级管理员）
                if (!"超级管理员".equals(stockIn.getCreatedByName())) {
                    System.err.println("测试失败: 入库单操作人姓名应为超级管理员, 实际: " + stockIn.getCreatedByName());
                } else {
                    System.out.println("入库单操作人姓名: " + stockIn.getCreatedByName());
                }
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(orderId, acceptanceId, null, null, null, null);
        }
    }

    /**
     * 测试初验合格时同步采购订单状态为"物料检验"
     * <p>
     * 采购订单置"运输中"生成验收单 → 确认到货 → 初验合格
     * → 断言关联采购订单状态同步变为"物料检验"
     * </p>
     */
    @Test
    public void testInspectPassSyncsPurchaseOrderStatus() {
        Long orderId = null;
        Long acceptanceId = null;
        try {
            PurchaseOrders order = createOrder();
            orderId = order.getId();
            AcceptanceOrder acceptance = setupTransitAcceptance(order);
            if (acceptance == null) {
                System.err.println("测试失败: 未生成验收单");
                return;
            }
            acceptanceId = acceptance.getAcceptanceId();

            // 确认到货（验收单 → 到货初验，采购订单同步到货初验）
            acceptanceOrderService.confirmArrival(acceptanceId);
            // 初验合格（验收单 → 物料检验，采购订单同步物料检验）
            acceptanceOrderService.inspect(acceptanceId, true, "外观完好");

            PurchaseOrders updatedOrder = purchaseOrdersMapper.selectById(orderId);
            if (updatedOrder == null || !"物料检验".equals(String.valueOf(updatedOrder.getStatus()))) {
                System.err.println("测试失败: 初验合格后采购订单应为物料检验, 实际: " + (updatedOrder == null ? "不存在" : updatedOrder.getStatus()));
            } else {
                System.out.println("初验合格后采购订单状态同步为: " + updatedOrder.getStatus());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(orderId, acceptanceId, null, null, null, null);
        }
    }

    /**
     * 测试初验不合格时同步采购订单状态为"待退货"
     * <p>
     * 采购订单置"运输中"生成验收单 → 确认到货 → 初验不合格
     * → 断言关联采购订单状态同步变为"待退货"
     * </p>
     */
    @Test
    public void testInspectFailSyncsPurchaseOrderStatus() {
        Long orderId = null;
        Long acceptanceId = null;
        try {
            PurchaseOrders order = createOrder();
            orderId = order.getId();
            AcceptanceOrder acceptance = setupTransitAcceptance(order);
            if (acceptance == null) {
                System.err.println("测试失败: 未生成验收单");
                return;
            }
            acceptanceId = acceptance.getAcceptanceId();

            acceptanceOrderService.confirmArrival(acceptanceId);
            acceptanceOrderService.inspect(acceptanceId, false, "包装破损");

            PurchaseOrders updatedOrder = purchaseOrdersMapper.selectById(orderId);
            if (updatedOrder == null || !"待退货".equals(String.valueOf(updatedOrder.getStatus()))) {
                System.err.println("测试失败: 初验不合格后采购订单应为待退货, 实际: " + (updatedOrder == null ? "不存在" : updatedOrder.getStatus()));
            } else {
                System.out.println("初验不合格后采购订单状态同步为: " + updatedOrder.getStatus());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(orderId, acceptanceId, null, null, null, null);
        }
    }

    /**
     * 测试检验不合格时同步采购订单状态为"待退货"
     * <p>
     * 采购订单置"运输中"生成验收单 → 确认到货 → 初验合格（物料检验）→ 检验不合格
     * → 断言关联采购订单状态同步变为"待退货"
     * </p>
     */
    @Test
    public void testQualityCheckFailSyncsPurchaseOrderStatus() {
        Long orderId = null;
        Long acceptanceId = null;
        try {
            PurchaseOrders order = createOrder();
            orderId = order.getId();
            AcceptanceOrder acceptance = setupTransitAcceptance(order);
            if (acceptance == null) {
                System.err.println("测试失败: 未生成验收单");
                return;
            }
            acceptanceId = acceptance.getAcceptanceId();

            acceptanceOrderService.confirmArrival(acceptanceId);
            acceptanceOrderService.inspect(acceptanceId, true, "外观完好");
            acceptanceOrderService.qualityCheck(acceptanceId, false, null, "含量不达标");

            PurchaseOrders updatedOrder = purchaseOrdersMapper.selectById(orderId);
            if (updatedOrder == null || !"待退货".equals(String.valueOf(updatedOrder.getStatus()))) {
                System.err.println("测试失败: 检验不合格后采购订单应为待退货, 实际: " + (updatedOrder == null ? "不存在" : updatedOrder.getStatus()));
            } else {
                System.out.println("检验不合格后采购订单状态同步为: " + updatedOrder.getStatus());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(orderId, acceptanceId, null, null, null, null);
        }
    }

    /**
     * 测试重新收货时同步采购订单状态为"到货初验"（跟随新验收单）
     * <p>
     * 采购订单置"运输中"生成验收单 → 确认到货 → 初验不合格（待退货）→ 重新收货
     * → 断言生成新验收单（到货初验）、原单标记已退换、采购订单状态同步为"到货初验"
     * </p>
     */
    @Test
    public void testReReceiveSyncsPurchaseOrderStatus() {
        Long orderId = null;
        try {
            PurchaseOrders order = createOrder();
            orderId = order.getId();
            AcceptanceOrder acceptance = setupTransitAcceptance(order);
            if (acceptance == null) {
                System.err.println("测试失败: 未生成验收单");
                return;
            }
            Long originalAcceptanceId = acceptance.getAcceptanceId();

            acceptanceOrderService.confirmArrival(originalAcceptanceId);
            acceptanceOrderService.inspect(originalAcceptanceId, false, "包装破损");
            // 重新收货：生成新验收单（到货初验），原单标记已退换
            AcceptanceOrder newAcceptance = acceptanceOrderService.reReceive(originalAcceptanceId);

            if (newAcceptance == null) {
                System.err.println("测试失败: 未生成新验收单");
                return;
            }
            if (!"到货初验".equals(newAcceptance.getStatus())) {
                System.err.println("测试失败: 新验收单状态应为到货初验, 实际: " + newAcceptance.getStatus());
            }
            AcceptanceOrder original = acceptanceOrderMapper.selectById(originalAcceptanceId);
            if (!"已退换".equals(original.getStatus())) {
                System.err.println("测试失败: 原验收单状态应为已退换, 实际: " + original.getStatus());
            }
            PurchaseOrders updatedOrder = purchaseOrdersMapper.selectById(orderId);
            if (updatedOrder == null || !"到货初验".equals(String.valueOf(updatedOrder.getStatus()))) {
                System.err.println("测试失败: 重新收货后采购订单应为到货初验, 实际: " + (updatedOrder == null ? "不存在" : updatedOrder.getStatus()));
            } else {
                System.out.println("重新收货后采购订单状态同步为: " + updatedOrder.getStatus());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 重新收货产生两张验收单，按采购订单号整体清理
            cleanup(orderId, null, null, null, null, null);
        }
    }

    /**
     * 测试验收合格入库时物料名称以物料主数据为权威来源
     * <p>
     * 即使验收明细 material_name 被误传为分类值（如"原料"），
     * 生成的入库单明细与库存记录的 item_name 也必须取物料主数据名称
     * </p>
     */
    @Test
    public void testQualityCheckInboundOverridesWrongMaterialName() {
        Long orderId = null;
        Long acceptanceId = null;
        try {
            PurchaseOrders order = createOrder();
            orderId = order.getId();
            AcceptanceOrder acceptance = setupTransitAcceptance(order);
            if (acceptance == null) {
                System.err.println("测试失败: 未生成验收单");
                return;
            }
            acceptanceId = acceptance.getAcceptanceId();
            List<AcceptanceDetail> details = acceptanceDetailMapper.selectList(
                    new QueryWrapper<AcceptanceDetail>().eq("acceptance_id", acceptanceId));
            AcceptanceDetail detail = details.get(0);
            // 模拟旧 bug：验收明细物料名称被误填为分类值
            detail.setMaterialName("原料");
            detail.setBatchNumber(TEST_BATCH);
            detail.setExpiryDate(LocalDate.now().plusYears(1));
            acceptanceDetailMapper.updateById(detail);
            AcceptanceOrder toInspect = new AcceptanceOrder();
            toInspect.setAcceptanceId(acceptanceId);
            toInspect.setStatus("物料检验");
            acceptanceOrderMapper.updateById(toInspect);

            StockIn stockIn = acceptanceOrderService.qualityCheck(acceptanceId, true, INBOUND_UNIT_ID, "测试合格");
            if (stockIn == null) {
                System.err.println("测试失败: 未返回自动生成的入库单");
                return;
            }
            // 入库单明细名称应为物料主数据名称，而非验收明细的分类值
            List<java.util.Map<String, Object>> detailRows = jdbcTemplate.queryForList(
                    "SELECT item_name, category_name FROM stock_in_detail WHERE in_id = ? AND is_deleted = 0", stockIn.getInId());
            if (detailRows.isEmpty()) {
                System.err.println("测试失败: 未生成入库单明细");
            } else {
                String actualName = String.valueOf(detailRows.get(0).get("item_name"));
                if (!TEST_ITEM_NAME.equals(actualName)) {
                    System.err.println("测试失败: 入库明细名称应为物料主数据 " + TEST_ITEM_NAME + ", 实际: " + actualName);
                } else {
                    System.out.println("入库明细名称正确（取物料主数据）: " + actualName);
                }
            }
            // 库存记录名称/分类应来自物料主数据
            List<java.util.Map<String, Object>> stockRows = jdbcTemplate.queryForList(
                    "SELECT stock_id, item_name, category_name FROM stock WHERE item_code = ? AND is_deleted = 0", TEST_ITEM_CODE);
            if (stockRows.isEmpty()) {
                System.err.println("测试失败: 未生成库存记录");
            } else {
                String stockName = String.valueOf(stockRows.get(0).get("item_name"));
                if (!TEST_ITEM_NAME.equals(stockName)) {
                    System.err.println("测试失败: 库存名称应为物料主数据 " + TEST_ITEM_NAME + ", 实际: " + stockName);
                } else {
                    System.out.println("库存名称正确（取物料主数据）: " + stockName);
                }
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(orderId, acceptanceId, null, null, null, null);
        }
    }

    /**
     * 测试库存流水携带绑定的入库单号与验收单（检验单）号
     * <p>
     * 验收合格入库后，按库存ID查询流水，入库来源流水应回显 inCode（入库单号）
     * 与 acceptanceCode（验收单号）
     * </p>
     */
    @Test
    public void testStockTransactionsCarryInboundAndAcceptanceCode() {
        Long orderId = null;
        Long acceptanceId = null;
        try {
            PurchaseOrders order = createOrder();
            orderId = order.getId();
            AcceptanceOrder acceptance = setupTransitAcceptance(order);
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

            StockIn stockIn = acceptanceOrderService.qualityCheck(acceptanceId, true, INBOUND_UNIT_ID, "测试合格");
            if (stockIn == null) {
                System.err.println("测试失败: 未返回自动生成的入库单");
                return;
            }
            List<java.util.Map<String, Object>> stockRows = jdbcTemplate.queryForList(
                    "SELECT stock_id FROM stock WHERE item_code = ? AND is_deleted = 0", TEST_ITEM_CODE);
            if (stockRows.isEmpty()) {
                System.err.println("测试失败: 未生成库存记录");
                return;
            }
            Long stockId = ((Number) stockRows.get(0).get("stock_id")).longValue();
            List<StockTransactionDto> transactions = stockService.getTransactionsByStockId(stockId);
            StockTransactionDto inbound = transactions.stream()
                    .filter(t -> "stock_in".equals(String.valueOf(t.getRelatedType())))
                    .findFirst().orElse(null);
            if (inbound == null) {
                System.err.println("测试失败: 未找到入库流水");
            } else {
                if (!stockIn.getInCode().equals(inbound.getInCode())) {
                    System.err.println("测试失败: 流水入库单号应为 " + stockIn.getInCode() + ", 实际: " + inbound.getInCode());
                } else {
                    System.out.println("流水入库单号: " + inbound.getInCode());
                }
                String expectedAcceptanceCode = acceptance.getAcceptanceCode();
                if (!expectedAcceptanceCode.equals(inbound.getAcceptanceCode())) {
                    System.err.println("测试失败: 流水验收单号应为 " + expectedAcceptanceCode + ", 实际: " + inbound.getAcceptanceCode());
                } else {
                    System.out.println("流水验收单号: " + inbound.getAcceptanceCode());
                }
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(orderId, acceptanceId, null, null, null, null);
        }
    }

    /**
     * 测试入库单查询接口回填操作人姓名与仓库名称
     * <p>
     * 验收合格自动入库后，通过 searchWithDetails 查询该入库单，
     * 返回结果应携带 createdByName（操作人姓名）与 warehouseName（仓库名称）
     * </p>
     */
    @Test
    public void testStockInSearchReturnsOperatorAndWarehouseName() {
        Long orderId = null;
        Long acceptanceId = null;
        try {
            PurchaseOrders order = createOrder();
            orderId = order.getId();
            AcceptanceOrder acceptance = setupTransitAcceptance(order);
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

            StockIn stockIn = acceptanceOrderService.qualityCheck(acceptanceId, true, INBOUND_UNIT_ID, "测试合格");
            if (stockIn == null) {
                System.err.println("测试失败: 未返回自动生成的入库单");
                return;
            }
            // 按入库单号查询（带明细子表）
            StockIn query = new StockIn();
            query.setInCode(stockIn.getInCode());
            PagedResult<StockInWithDetailsDto> result = stockInService.searchWithDetails(
                    query, null, null, null, null, null, null, 0, 10);
            if (result.getItems().isEmpty()) {
                System.err.println("测试失败: 未查询到入库单 " + stockIn.getInCode());
                return;
            }
            StockInWithDetailsDto dto = result.getItems().get(0);
            if (!stockIn.getInCode().equals(dto.getInCode())) {
                System.err.println("测试失败: 查询入库单号不符: " + dto.getInCode());
            } else {
                System.out.println("查询入库单号: " + dto.getInCode());
            }
            if (!"超级管理员".equals(dto.getCreatedByName())) {
                System.err.println("测试失败: 操作人姓名应为超级管理员, 实际: " + dto.getCreatedByName());
            } else {
                System.out.println("查询操作人姓名: " + dto.getCreatedByName());
            }
            if (!"耒阳制剂室".equals(dto.getWarehouseName())) {
                System.err.println("测试失败: 仓库名称应为耒阳制剂室, 实际: " + dto.getWarehouseName());
            } else {
                System.out.println("查询仓库名称: " + dto.getWarehouseName());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(orderId, acceptanceId, null, null, null, null);
        }
    }

    /**
     * 测试验收单高级查询（带明细）返回仓库名称（warehouseName）
     * <p>
     * 验收单创建时已携带生产单位ID，searchWithDetails 应解析出仓库名称
     * </p>
     */
    @Test
    public void testAcceptanceSearchReturnsWarehouseName() {
        Long orderId = null;
        Long acceptanceId = null;
        try {
            PurchaseOrders order = createOrder();
            orderId = order.getId();
            AcceptanceOrder acceptance = setupTransitAcceptance(order);
            if (acceptance == null) {
                System.err.println("测试失败: 未生成验收单");
                return;
            }
            acceptanceId = acceptance.getAcceptanceId();

            // 高级查询验收单（带明细），断言仓库名称被回填
            AcceptanceOrder query = new AcceptanceOrder();
            query.setAcceptanceId(acceptanceId);
            PagedResult<AcceptanceWithDetailsDto> result = acceptanceOrderService.searchWithDetails(
                    query, null, 0, 10);
            if (result.getItems().isEmpty()) {
                System.err.println("测试失败: 未查询到验收单");
                return;
            }
            AcceptanceWithDetailsDto dto = result.getItems().get(0);
            if (!acceptance.getAcceptanceId().equals(dto.getAcceptanceId())) {
                System.err.println("测试失败: 查询验收单ID不符: " + dto.getAcceptanceId());
            } else {
                System.out.println("查询验收单编号: " + dto.getAcceptanceCode());
            }
            if (!"耒阳制剂室".equals(dto.getWarehouseName())) {
                System.err.println("测试失败: 仓库名称应为耒阳制剂室, 实际: " + dto.getWarehouseName());
            } else {
                System.out.println("验收查询仓库名称: " + dto.getWarehouseName());
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
        // 清理历史残留的同编码物料（避免唯一索引冲突）
        jdbcTemplate.update("DELETE FROM material WHERE material_code = ?", TEST_ITEM_CODE);

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

        // 创建物料主数据（验收单物料名称应以物料表为准）
        Material material = new Material();
        material.setMaterialCode(TEST_ITEM_CODE);
        material.setMaterialName(TEST_ITEM_NAME);
        material.setCategoryName("原料");
        material.setUnitName("kg");
        material.setMaterialStatus(1);
        materialMapper.insert(material);

        PurchaseOrderItems item = new PurchaseOrderItems();
        item.setOrderId(order.getId());
        item.setSequenceNumber(1);
        item.setMaterialId(material.getMaterialId());
        item.setMaterialCode(TEST_ITEM_CODE);
        // 制剂名称存分类、原药材品名为陈旧值，均与物料主数据不同，用于验证物料表名称优先
        item.setProductName("原料");
        item.setRawMaterialName(STALE_RAW_NAME);
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
     * 创建采购订单并置"运输中"，返回自动生成的验收单
     *
     * @param order 已创建的采购订单（含明细）
     * @return 自动生成的验收单，未生成时返回null
     */
    private AcceptanceOrder setupTransitAcceptance(PurchaseOrders order) {
        PurchaseOrders update = new PurchaseOrders();
        update.setId(order.getId());
        update.setStatus("运输中");
        purchaseOrdersService.updatePurchaseOrder(update);
        return acceptanceOrderMapper.selectOne(
                new QueryWrapper<AcceptanceOrder>().eq("purchase_number", testOrderNo));
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
            // 5. 清理物料主数据（purchase_order_items.material_id 无外键约束，可安全删除）
            jdbcTemplate.update("DELETE FROM material WHERE material_code = ?", TEST_ITEM_CODE);
        } catch (Exception e) {
            System.err.println("清理测试数据失败: " + e.getMessage());
        }
    }

    // endregion
}