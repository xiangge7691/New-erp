package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.Warehouse.AvailableOutOrderDto;
import com.tonghui.erp.Common.Dto.Warehouse.OutOrderMaterialDto;
import com.tonghui.erp.Common.Dto.Warehouse.ReturnItemRequest;
import com.tonghui.erp.Common.Dto.Warehouse.ReturnOrderCreateDto;
import com.tonghui.erp.Common.Dto.Stock.StockOutWithDetailsDto;
import com.tonghui.erp.Data.Entity.ReturnOrder;
import com.tonghui.erp.Data.Entity.ReturnOrderDetail;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Data.Entity.StockTransaction;
import com.tonghui.erp.Data.mapper.ReturnOrderDetailMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.StockOutDetailMapper;
import com.tonghui.erp.Data.mapper.StockOutMapper;
import com.tonghui.erp.Data.mapper.StockTransactionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 退库单服务接口测试
 * <p>
 * 覆盖退库单的分页查询、可退库出库单/出库物料查询、
 * 新增退库（库存回增与流水生成、库存重建）及异常分支（超可退数量退库）
 * </p>
 */
@SpringBootTest
public class ReturnOrderServiceTest {

    // region 常量与依赖注入
    // ===================================
    // 常量与依赖注入
    // ===================================

    /** 测试物料编码（专用，避免与业务数据冲突） */
    private static final String TEST_ITEM_CODE = "TESTR001";
    /** 测试物料名称 */
    private static final String TEST_ITEM_NAME = "退库测试物料";
    /** 测试物料分类 */
    private static final String TEST_CATEGORY = "测试分类";
    /** 测试物料单位 */
    private static final String TEST_UNIT = "kg";
    /** 测试批次号 */
    private static final String TEST_BATCH = "RBATCH01";
    /** 测试出库单号（动态唯一，避免软删除残留与唯一索引冲突） */
    private final String testOutCode = "TESTCK" + System.currentTimeMillis();
    /** 测试生产计划编号（动态唯一） */
    private final String testPlanNo = "TESTPLANR" + System.currentTimeMillis();
    /** 测试生产计划名称 */
    private static final String TEST_PLAN_NAME = "退库测试计划单";
    /** 仓库名称（原料仓） */
    private static final String WAREHOUSE = "原料仓";
    /** 仓库ID（原料仓） */
    private static final Long WAREHOUSE_UNIT_ID = 19L;

    /** 退库单服务 */
    @Autowired
    private ReturnOrderService returnOrderService;
    /** 库存Mapper */
    @Autowired
    private StockMapper stockMapper;
    /** 库存流水Mapper */
    @Autowired
    private StockTransactionMapper stockTransactionMapper;
    /** 退库明细Mapper */
    @Autowired
    private ReturnOrderDetailMapper returnOrderDetailMapper;
    /** 出库单Mapper */
    @Autowired
    private StockOutMapper stockOutMapper;
    /** 出库明细Mapper */
    @Autowired
    private StockOutDetailMapper stockOutDetailMapper;
    /** 生产计划Mapper（创建生产计划主数据以验证计划名称回填） */
    @Autowired
    private com.tonghui.erp.Data.mapper.ProductionPlanMapper productionPlanMapper;
    /** 出库单服务（验证出库明细返回仓库名称） */
    @Autowired
    private StockOutService stockOutService;
    /** JdbcTemplate（用于物理删除测试数据，绕开软删除） */
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // endregion

    // region 测试方法
    // ===================================
    // 测试方法
    // ===================================

    /**
     * 测试分页查询退库单列表（空参数不抛异常）
     */
    @Test
    public void testQueryReturnOrders() {
        try {
            Page<ReturnOrder> page = returnOrderService.queryReturnOrders(null, 0, 10);
            System.out.println("退库单总数: " + page.getTotal());
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试可退库出库单与出库物料查询
     * <p>
     * 造出库单（生产领料出库3.0）：可退总量应为3.0，物料可退数量应为3.0
     * </p>
     */
    @Test
    public void testGetAvailableOutOrdersAndMaterials() {
        Long stockId = null;
        Long outId = null;
        try {
            Stock stock = createStock(new BigDecimal("5.0"));
            stockId = stock.getStockId();
            outId = createStockOut(stock.getStockId(), new BigDecimal("3.0"));

            // 可退库出库单列表
            List<AvailableOutOrderDto> outOrders = returnOrderService.getAvailableOutOrders();
            AvailableOutOrderDto match = outOrders.stream()
                    .filter(o -> testOutCode.equals(o.getOutOrderNo())).findFirst().orElse(null);
            if (match == null) {
                System.err.println("测试失败: 可退出库单列表未包含测试出库单");
            } else {
                System.out.println("出库单: " + match.getOutOrderNo() + " 可退总量: " + match.getTotalAvailableQuantity());
                if (match.getTotalAvailableQuantity().compareTo(new BigDecimal("3.0")) != 0) {
                    System.err.println("测试失败: 可退总量应为3.0, 实际: " + match.getTotalAvailableQuantity());
                }
            }

            // 出库物料明细
            List<OutOrderMaterialDto> materials = returnOrderService.getOutOrderMaterials(testOutCode);
            if (materials.isEmpty()) {
                System.err.println("测试失败: 出库物料明细为空");
            } else {
                OutOrderMaterialDto m = materials.get(0);
                System.out.println("物料标识: " + m.getInventoryKey() + " 可退数量: " + m.getAvailableQuantity());
                if (m.getAvailableQuantity().compareTo(new BigDecimal("3.0")) != 0) {
                    System.err.println("测试失败: 物料可退数量应为3.0, 实际: " + m.getAvailableQuantity());
                }
                String expectedKey = TEST_ITEM_CODE + "_" + WAREHOUSE + "_" + TEST_BATCH;
                if (!expectedKey.equals(m.getInventoryKey())) {
                    System.err.println("测试失败: 库存标识应为: " + expectedKey + ", 实际: " + m.getInventoryKey());
                }
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(stockId, outId, null);
        }
    }

    /**
     * 测试新增退库单成功流程（原库存行存在，数量累加）
     * <p>
     * 造库存5.0、出库明细3.0，退库1.0：库存应变为6.0、
     * 生成退库流水+1.0、可退数量变为2.0
     * </p>
     */
    @Test
    public void testCreateReturnOrder() {
        Long stockId = null;
        Long outId = null;
        Long orderId = null;
        try {
            Stock stock = createStock(new BigDecimal("5.0"));
            stockId = stock.getStockId();
            outId = createStockOut(stock.getStockId(), new BigDecimal("3.0"));

            ReturnOrderCreateDto dto = new ReturnOrderCreateDto();
            dto.setOutOrderNo(testOutCode);
            dto.setRemark("接口测试退库");
            ReturnItemRequest item = new ReturnItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + WAREHOUSE + "_" + TEST_BATCH);
            item.setReturnQuantity(new BigDecimal("1.0"));
            dto.setItems(List.of(item));

            ReturnOrder order = returnOrderService.createReturnOrder(dto);
            orderId = order.getId();
            System.out.println("退库单号: " + order.getReturnNo());

            // 断言单号前缀
            if (!order.getReturnNo().startsWith("TK-")) {
                System.err.println("测试失败: 退库单号前缀错误: " + order.getReturnNo());
            }
            // 断言库存累加为6.0
            Stock updated = stockMapper.selectById(stockId);
            if (updated.getQuantity().compareTo(new BigDecimal("6.0")) != 0) {
                System.err.println("测试失败: 退库后库存应为6.0, 实际: " + updated.getQuantity());
            }
            // 断言生成1条退库流水（+1.0）
            List<StockTransaction> txs = stockTransactionMapper.selectList(new QueryWrapper<StockTransaction>()
                    .eq("related_type", "return").eq("related_id", orderId));
            if (txs.size() != 1) {
                System.err.println("测试失败: 退库流水应为1条, 实际: " + txs.size());
            } else {
                System.out.println("退库流水: " + txs.get(0).getTransactionType() + " 数量变化: " + txs.get(0).getQuantityChange());
                if (txs.get(0).getQuantityChange().compareTo(new BigDecimal("1.0")) != 0) {
                    System.err.println("测试失败: 退库流水数量变化应为+1.0");
                }
            }
            // 断言可退数量变为2.0
            OutOrderMaterialDto m = returnOrderService.getOutOrderMaterials(testOutCode).get(0);
            if (m.getAvailableQuantity().compareTo(new BigDecimal("2.0")) != 0) {
                System.err.println("测试失败: 可退数量应为2.0, 实际: " + m.getAvailableQuantity());
            } else {
                System.out.println("退库后可退数量: " + m.getAvailableQuantity());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(stockId, outId, orderId);
        }
    }

    /**
     * 测试新增退库单时原库存行已删除（库存重建）
     * <p>
     * 出库后库存行被删（模拟出库清零删除），退库1.0应重建库存行1.0
     * </p>
     */
    @Test
    public void testCreateReturnOrderRebuildStock() {
        Long stockId = null;
        Long outId = null;
        Long orderId = null;
        try {
            Stock stock = createStock(new BigDecimal("5.0"));
            stockId = stock.getStockId();
            outId = createStockOut(stock.getStockId(), new BigDecimal("3.0"));
            // 删除库存行，模拟出库清零后删除
            stockMapper.deleteById(stockId);

            ReturnOrderCreateDto dto = new ReturnOrderCreateDto();
            dto.setOutOrderNo(testOutCode);
            ReturnItemRequest item = new ReturnItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + WAREHOUSE + "_" + TEST_BATCH);
            item.setReturnQuantity(new BigDecimal("1.0"));
            dto.setItems(List.of(item));

            ReturnOrder order = returnOrderService.createReturnOrder(dto);
            orderId = order.getId();
            System.out.println("退库单号: " + order.getReturnNo());

            // 断言库存重建为1.0
            Stock rebuilt = stockMapper.selectOne(new QueryWrapper<Stock>()
                    .eq("prod_unit_id", WAREHOUSE_UNIT_ID)
                    .eq("item_code", TEST_ITEM_CODE)
                    .eq("batch_number", TEST_BATCH)
                    .eq("is_deleted", 0));
            if (rebuilt == null) {
                System.err.println("测试失败: 退库后未重建库存行");
            } else if (rebuilt.getQuantity().compareTo(new BigDecimal("1.0")) != 0) {
                System.err.println("测试失败: 重建库存应为1.0, 实际: " + rebuilt.getQuantity());
            } else {
                System.out.println("重建库存数量: " + rebuilt.getQuantity());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(stockId, outId, orderId);
        }
    }

    /**
     * 测试超可退数量退库（应抛出异常）
     */
    @Test
    public void testCreateReturnOrderOverAvailable() {
        Long stockId = null;
        Long outId = null;
        try {
            Stock stock = createStock(new BigDecimal("5.0"));
            stockId = stock.getStockId();
            outId = createStockOut(stock.getStockId(), new BigDecimal("3.0"));

            ReturnOrderCreateDto dto = new ReturnOrderCreateDto();
            dto.setOutOrderNo(testOutCode);
            ReturnItemRequest item = new ReturnItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + WAREHOUSE + "_" + TEST_BATCH);
            item.setReturnQuantity(new BigDecimal("4.0"));
            dto.setItems(List.of(item));

            try {
                returnOrderService.createReturnOrder(dto);
                System.err.println("测试失败: 超可退数量退库未抛出异常");
            } catch (RuntimeException e) {
                System.out.println("超可退数量正确抛出异常: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(stockId, outId, null);
        }
    }

    /**
     * 测试退库单列表查询回填生产计划名称（productionPlanName）
     * <p>
     * 创建带生产计划编号的退库单，列表查询时按编号关联 production_plan 解析计划名称
     * </p>
     */
    @Test
    public void testQueryReturnOrdersFillsProductionPlanName() {
        Long stockId = null;
        Long outId = null;
        Long orderId = null;
        try {
            // 创建生产计划主数据（用于解析计划名称）
            com.tonghui.erp.Data.Entity.ProductionPlan plan = new com.tonghui.erp.Data.Entity.ProductionPlan();
            plan.setPlanNumber(testPlanNo);
            plan.setPlanName(TEST_PLAN_NAME);
            plan.setPreparationCode("TESTP1");
            plan.setPreparationName("测试制剂");
            plan.setPlanQuantity(new BigDecimal("1"));
            plan.setPlanType("生产计划");
            productionPlanMapper.insert(plan);

            Stock stock = createStock(new BigDecimal("5.0"));
            stockId = stock.getStockId();
            outId = createStockOut(stock.getStockId(), new BigDecimal("3.0"));

            ReturnOrderCreateDto dto = new ReturnOrderCreateDto();
            dto.setOutOrderNo(testOutCode);
            ReturnItemRequest item = new ReturnItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + WAREHOUSE + "_" + TEST_BATCH);
            item.setReturnQuantity(new BigDecimal("1.0"));
            dto.setItems(List.of(item));
            ReturnOrder order = returnOrderService.createReturnOrder(dto);
            orderId = order.getId();

            // 列表查询，断言生产计划名称被解析回填
            Page<ReturnOrder> page = returnOrderService.queryReturnOrders(testOutCode, 0, 10);
            ReturnOrder hit = page.getRecords().stream()
                    .filter(r -> testOutCode.equals(r.getOutOrderNo()))
                    .findFirst().orElse(null);
            if (hit == null) {
                System.err.println("测试失败: 未查询到退库单");
            } else if (!TEST_PLAN_NAME.equals(hit.getProductionPlanName())) {
                System.err.println("测试失败: 生产计划名称应为 " + TEST_PLAN_NAME + ", 实际: " + hit.getProductionPlanName());
            } else {
                System.out.println("退库单生产计划名称: " + hit.getProductionPlanName());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(stockId, outId, orderId);
            jdbcTemplate.update("DELETE FROM production_plan WHERE plan_number = ?", testPlanNo);
        }
    }

    /**
     * 测试出库单高级查询（带明细）返回明细仓库名称（warehouseName）
     * <p>
     * 出库明细携带生产单位ID，searchWithDetails 应解析出仓库名称
     * </p>
     */
    @Test
    public void testStockOutSearchReturnsDetailWarehouseName() {
        Long stockId = null;
        Long outId = null;
        try {
            Stock stock = createStock(new BigDecimal("5.0"));
            stockId = stock.getStockId();
            outId = createStockOut(stock.getStockId(), new BigDecimal("3.0"));

            // 高级查询出库单（带明细），断言明细仓库名称被回填
            StockOut query = new StockOut();
            query.setOutCode(testOutCode);
            PagedResult<StockOutWithDetailsDto> result = stockOutService.searchWithDetails(
                    query, null, null, null, null, null, null, null, 0, 10);
            if (result.getItems().isEmpty()) {
                System.err.println("测试失败: 未查询到出库单 " + testOutCode);
                return;
            }
            StockOutWithDetailsDto dto = result.getItems().get(0);
            if (!testOutCode.equals(dto.getOutCode())) {
                System.err.println("测试失败: 查询出库单号不符: " + dto.getOutCode());
            } else {
                System.out.println("查询出库单号: " + dto.getOutCode());
            }
            if (dto.getDetails().isEmpty()) {
                System.err.println("测试失败: 出库明细为空");
            } else {
                String name = dto.getDetails().get(0).getWarehouseName();
                if (!WAREHOUSE.equals(name)) {
                    System.err.println("测试失败: 明细仓库名称应为 " + WAREHOUSE + ", 实际: " + name);
                } else {
                    System.out.println("明细仓库名称: " + name);
                }
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(stockId, outId, null);
        }
    }

    /**
     * 测试出库单查询（/stockout/search）返回操作人姓名（createdByName）
     * <p>
     * 出库单创建时无登录态 createdBy=1，查询应解析出用户姓名"超级管理员"
     * </p>
     */
    @Test
    public void testStockOutSearchReturnsOperatorName() {
        Long stockId = null;
        Long outId = null;
        try {
            Stock stock = createStock(new BigDecimal("5.0"));
            stockId = stock.getStockId();
            outId = createStockOut(stock.getStockId(), new BigDecimal("3.0"));

            StockOut query = new StockOut();
            query.setOutCode(testOutCode);
            Page<StockOut> page = stockOutService.queryStockOuts(query, null, null, null, null, null, null, null, 0, 10);
            StockOut hit = page.getRecords().stream()
                    .filter(r -> testOutCode.equals(r.getOutCode()))
                    .findFirst().orElse(null);
            if (hit == null) {
                System.err.println("测试失败: 未查询到出库单");
            } else if (!"超级管理员".equals(hit.getCreatedByName())) {
                System.err.println("测试失败: 出库单操作人姓名应为超级管理员, 实际: " + hit.getCreatedByName());
            } else {
                System.out.println("出库单操作人姓名: " + hit.getCreatedByName());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(stockId, outId, null);
        }
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 创建测试库存记录
     *
     * @param quantity 库存数量
     * @return 创建的库存记录
     */
    private Stock createStock(BigDecimal quantity) {
        Stock stock = new Stock();
        stock.setProdUnitId(WAREHOUSE_UNIT_ID);
        stock.setItemType("material");
        stock.setItemCode(TEST_ITEM_CODE);
        stock.setItemName(TEST_ITEM_NAME);
        stock.setCategoryName(TEST_CATEGORY);
        stock.setUnitName(TEST_UNIT);
        stock.setBatchNumber(TEST_BATCH);
        stock.setQuantity(quantity);
        stock.setUnitPrice(new BigDecimal("10.00"));
        stockMapper.insert(stock);
        return stock;
    }

    /**
     * 创建测试出库单与出库明细（生产领料出库）
     *
     * @param stockId  库存ID
     * @param quantity 出库数量
     * @return 出库单ID
     */
    private Long createStockOut(Long stockId, BigDecimal quantity) {
        StockOut out = new StockOut();
        out.setOutCode(testOutCode);
        out.setOutType("生产领料出库");
        out.setProdUnitId(WAREHOUSE_UNIT_ID);
        out.setPlanNumber(testPlanNo);
        out.setOutDate(LocalDateTime.now());
        out.setOutStatus("已出库");
        out.setTotalAmount(new BigDecimal("0.00"));
        stockOutMapper.insert(out);

        StockOutDetail detail = new StockOutDetail();
        detail.setOutId(out.getOutId());
        detail.setProdUnitId(WAREHOUSE_UNIT_ID);
        detail.setStockId(stockId);
        detail.setItemType("material");
        detail.setItemCode(TEST_ITEM_CODE);
        detail.setItemName(TEST_ITEM_NAME);
        detail.setCategoryName(TEST_CATEGORY);
        detail.setUnitName(TEST_UNIT);
        detail.setBatchNumber(TEST_BATCH);
        detail.setQuantity(quantity);
        detail.setUnitPrice(new BigDecimal("10.00"));
        detail.setAmount(quantity.multiply(new BigDecimal("10.00")));
        stockOutDetailMapper.insert(detail);
        return out.getOutId();
    }

    /**
     * 清理测试数据（物理删除：明细→主表→流水→出库明细→出库单→库存）
     *
     * @param stockId 库存ID（可为null）
     * @param outId   出库单ID（可为null）
     * @param orderId 退库单ID（可为null）
     */
    private void cleanup(Long stockId, Long outId, Long orderId) {
        try {
            if (orderId != null) {
                jdbcTemplate.update("DELETE FROM return_order_detail WHERE return_order_id = ?", orderId);
                jdbcTemplate.update("DELETE FROM return_order WHERE id = ?", orderId);
                jdbcTemplate.update("DELETE FROM stock_transaction WHERE related_type = 'return' AND related_id = ?", orderId);
            }
            if (outId != null) {
                jdbcTemplate.update("DELETE FROM stock_out_detail WHERE out_id = ?", outId);
                jdbcTemplate.update("DELETE FROM stock_out WHERE out_id = ?", outId);
            }
            jdbcTemplate.update("DELETE FROM stock WHERE item_code = ?", TEST_ITEM_CODE);
            jdbcTemplate.update("DELETE FROM stock_transaction WHERE batch_number = ?", TEST_BATCH);
            if (stockId != null) {
                jdbcTemplate.update("DELETE FROM stock_transaction WHERE stock_id = ?", stockId);
            }
        } catch (Exception e) {
            System.err.println("清理测试数据失败: " + e.getMessage());
        }
    }

    // endregion
}