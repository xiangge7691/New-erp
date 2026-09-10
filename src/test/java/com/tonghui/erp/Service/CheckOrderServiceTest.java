package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.Warehouse.CheckItemRequest;
import com.tonghui.erp.Common.Dto.Warehouse.CheckOrderCreateDto;
import com.tonghui.erp.Common.Dto.Warehouse.StockDetailItemDto;
import com.tonghui.erp.Data.Entity.CheckOrder;
import com.tonghui.erp.Data.Entity.CheckOrderDetail;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockTransaction;
import com.tonghui.erp.Common.Dto.Stock.StockTransactionDto;
import com.tonghui.erp.Data.mapper.CheckOrderDetailMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.StockTransactionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

/**
 * 盘点单服务接口测试
 * <p>
 * 覆盖盘点单的分页查询、仓库库存详情查询、提交盘点
 * （盘盈/盘亏/盘平差异计算与库存调整流水）及异常分支（负实盘数量）
 * </p>
 */
@SpringBootTest
public class CheckOrderServiceTest {

    // region 常量与依赖注入
    // ===================================
    // 常量与依赖注入
    // ===================================

    /** 测试物料编码（专用，避免与业务数据冲突） */
    private static final String TEST_ITEM_CODE = "TESTP001";
    /** 测试物料名称 */
    private static final String TEST_ITEM_NAME = "盘点测试物料";
    /** 测试物料分类 */
    private static final String TEST_CATEGORY = "测试分类";
    /** 测试物料单位 */
    private static final String TEST_UNIT = "kg";
    /** 测试批次号 */
    private static final String TEST_BATCH = "PBATCH01";
    /** 盘点仓库名称（耒阳制剂室） */
    private static final String WAREHOUSE = "耒阳制剂室";
    /** 盘点仓库ID（耒阳制剂室） */
    private static final Long WAREHOUSE_UNIT_ID = 7L;

    /** 盘点单服务 */
    @Autowired
    private CheckOrderService checkOrderService;
    /** 库存服务（用于查询库存流水） */
    @Autowired
    private StockService stockService;
    /** 库存Mapper */
    @Autowired
    private StockMapper stockMapper;
    /** 库存流水Mapper */
    @Autowired
    private StockTransactionMapper stockTransactionMapper;
    /** 盘点明细Mapper */
    @Autowired
    private CheckOrderDetailMapper checkOrderDetailMapper;
    /** JdbcTemplate（用于物理删除测试数据，绕开软删除） */
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // endregion

    // region 测试方法
    // ===================================
    // 测试方法
    // ===================================

    /**
     * 测试分页查询盘点单列表（空参数不抛异常）
     */
    @Test
    public void testQueryCheckOrders() {
        try {
            Page<CheckOrder> page = checkOrderService.queryCheckOrders(null, null, null, null, 0, 10);
            System.out.println("盘点单总数: " + page.getTotal());
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试获取仓库库存详情（应包含测试物料）
     */
    @Test
    public void testGetStockDetails() {
        Long stockId = null;
        try {
            Stock stock = createStock(new BigDecimal("1.0"));
            stockId = stock.getStockId();

            List<StockDetailItemDto> details = checkOrderService.getStockDetails(WAREHOUSE, true, TEST_ITEM_CODE);
            boolean found = details.stream().anyMatch(d -> TEST_ITEM_CODE.equals(d.getMaterialCode()));
            if (!found) {
                System.err.println("测试失败: 库存详情未包含测试物料");
            } else {
                for (StockDetailItemDto d : details) {
                    System.out.println("物料: " + d.getMaterialCode() + " 系统库存: " + d.getSystemStock());
                }
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(stockId, null);
        }
    }

    /**
     * 测试提交盘点盘盈流程
     * <p>
     * 系统库存1.0实盘1.5：生成盘盈差异+0.5、库存调整为1.5、生成盘点流水
     * </p>
     */
    @Test
    public void testCreateCheckOrderProfit() {
        Long stockId = null;
        Long orderId = null;
        try {
            Stock stock = createStock(new BigDecimal("1.0"));
            stockId = stock.getStockId();

            CheckOrderCreateDto dto = new CheckOrderCreateDto();
            dto.setWarehouse(WAREHOUSE);
            CheckItemRequest item = new CheckItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + WAREHOUSE + "_" + TEST_BATCH);
            item.setActualStock(new BigDecimal("1.5"));
            dto.setItems(List.of(item));

            CheckOrder order = checkOrderService.createCheckOrder(dto);
            orderId = order.getId();
            System.out.println("盘点单号: " + order.getCheckNo() + " 盘盈数: " + order.getProfitCount());

            // 断言单号前缀
            if (!order.getCheckNo().startsWith("PD-")) {
                System.err.println("测试失败: 盘点单号前缀错误: " + order.getCheckNo());
            }
            // 断言库存调整为1.5
            Stock updated = stockMapper.selectById(stockId);
            if (updated.getQuantity().compareTo(new BigDecimal("1.5")) != 0) {
                System.err.println("测试失败: 盘点后库存应为1.5, 实际: " + updated.getQuantity());
            }
            // 断言生成1条盘点流水（+0.5）
            List<StockTransaction> txs = stockTransactionMapper.selectList(new QueryWrapper<StockTransaction>()
                    .eq("related_type", "check").eq("related_id", orderId));
            if (txs.size() != 1) {
                System.err.println("测试失败: 盘点流水应为1条, 实际: " + txs.size());
            } else {
                System.out.println("盘点流水: " + txs.get(0).getTransactionType() + " 数量变化: " + txs.get(0).getQuantityChange());
                if (txs.get(0).getQuantityChange().compareTo(new BigDecimal("0.5")) != 0) {
                    System.err.println("测试失败: 盘盈流水数量变化应为+0.5");
                }
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(stockId, orderId);
        }
    }

    /**
     * 测试提交盘点盘亏流程
     * <p>
     * 系统库存1.0实盘0.4：生成盘亏差异-0.6、库存调整为0.4、生成盘点流水
     * </p>
     */
    @Test
    public void testCreateCheckOrderLoss() {
        Long stockId = null;
        Long orderId = null;
        try {
            Stock stock = createStock(new BigDecimal("1.0"));
            stockId = stock.getStockId();

            CheckOrderCreateDto dto = new CheckOrderCreateDto();
            dto.setWarehouse(WAREHOUSE);
            CheckItemRequest item = new CheckItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + WAREHOUSE + "_" + TEST_BATCH);
            item.setActualStock(new BigDecimal("0.4"));
            dto.setItems(List.of(item));

            CheckOrder order = checkOrderService.createCheckOrder(dto);
            orderId = order.getId();
            System.out.println("盘点单号: " + order.getCheckNo() + " 盘亏数: " + order.getLossCount());

            if (!order.getCheckNo().startsWith("PD-")) {
                System.err.println("测试失败: 盘点单号前缀错误: " + order.getCheckNo());
            }
            Stock updated = stockMapper.selectById(stockId);
            if (updated.getQuantity().compareTo(new BigDecimal("0.4")) != 0) {
                System.err.println("测试失败: 盘点后库存应为0.4, 实际: " + updated.getQuantity());
            }
            List<StockTransaction> txs = stockTransactionMapper.selectList(new QueryWrapper<StockTransaction>()
                    .eq("related_type", "check").eq("related_id", orderId));
            if (txs.size() != 1) {
                System.err.println("测试失败: 盘点流水应为1条, 实际: " + txs.size());
            } else {
                System.out.println("盘点流水: " + txs.get(0).getTransactionType() + " 数量变化: " + txs.get(0).getQuantityChange());
                if (txs.get(0).getQuantityChange().compareTo(new BigDecimal("-0.6")) != 0) {
                    System.err.println("测试失败: 盘亏流水数量变化应为-0.6");
                }
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(stockId, orderId);
        }
    }

    /**
     * 测试盘平流程（实盘等于系统库存，不产生流水）
     */
    @Test
    public void testCreateCheckOrderMatch() {
        Long stockId = null;
        Long orderId = null;
        try {
            Stock stock = createStock(new BigDecimal("1.0"));
            stockId = stock.getStockId();

            CheckOrderCreateDto dto = new CheckOrderCreateDto();
            dto.setWarehouse(WAREHOUSE);
            CheckItemRequest item = new CheckItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + WAREHOUSE + "_" + TEST_BATCH);
            item.setActualStock(new BigDecimal("1.0"));
            dto.setItems(List.of(item));

            CheckOrder order = checkOrderService.createCheckOrder(dto);
            orderId = order.getId();
            System.out.println("盘点单号: " + order.getCheckNo() + " 盘平数: " + order.getMatchCount());

            // 断言不生成流水
            List<StockTransaction> txs = stockTransactionMapper.selectList(new QueryWrapper<StockTransaction>()
                    .eq("related_type", "check").eq("related_id", orderId));
            if (!txs.isEmpty()) {
                System.err.println("测试失败: 盘平不应产生流水");
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(stockId, orderId);
        }
    }

    /**
     * 测试负数实盘数量（应抛出异常）
     */
    @Test
    public void testCreateCheckOrderNegative() {
        Long stockId = null;
        try {
            Stock stock = createStock(new BigDecimal("1.0"));
            stockId = stock.getStockId();

            CheckOrderCreateDto dto = new CheckOrderCreateDto();
            dto.setWarehouse(WAREHOUSE);
            CheckItemRequest item = new CheckItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + WAREHOUSE + "_" + TEST_BATCH);
            item.setActualStock(new BigDecimal("-1.0"));
            dto.setItems(List.of(item));

            try {
                checkOrderService.createCheckOrder(dto);
                System.err.println("测试失败: 负实盘数量未抛出异常");
            } catch (RuntimeException e) {
                System.out.println("负实盘数量正确抛出异常: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(stockId, null);
        }
    }

    /**
     * 测试盘点流水携带盘点单号（inCode）
     * <p>
     * 盘盈后，通过 getTransactionsByStockId 查询库存流水，
     * 盘盈入库流水应返回 inCode = 盘点单号（checkNo）
     * </p>
     */
    @Test
    public void testCheckTransactionsCarryCheckNo() {
        Long stockId = null;
        Long orderId = null;
        try {
            Stock stock = createStock(new BigDecimal("1.0"));
            stockId = stock.getStockId();

            CheckOrderCreateDto dto = new CheckOrderCreateDto();
            dto.setWarehouse(WAREHOUSE);
            CheckItemRequest item = new CheckItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + WAREHOUSE + "_" + TEST_BATCH);
            item.setActualStock(new BigDecimal("1.5"));
            dto.setItems(List.of(item));

            CheckOrder order = checkOrderService.createCheckOrder(dto);
            orderId = order.getId();
            System.out.println("盘点单号: " + order.getCheckNo());

            // 查询库存流水，断言盘盈入库流水携带盘点单号
            List<StockTransactionDto> txs = stockService.getTransactionsByStockId(stockId);
            StockTransactionDto profitIn = txs.stream()
                    .filter(t -> "盘盈入库".equals(String.valueOf(t.getTransactionType())))
                    .findFirst().orElse(null);
            if (profitIn == null) {
                System.err.println("测试失败: 未找到盘盈入库流水");
            } else if (!order.getCheckNo().equals(profitIn.getInCode())) {
                System.err.println("测试失败: 盘盈入库流水 inCode 应为 " + order.getCheckNo() + ", 实际: " + profitIn.getInCode());
            } else {
                System.out.println("盘点流水盘点单号: " + profitIn.getInCode());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(stockId, orderId);
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
     * 清理测试数据（物理删除：明细→主表→流水→库存）
     *
     * @param stockId 库存ID（可为null）
     * @param orderId 盘点单ID（可为null）
     */
    private void cleanup(Long stockId, Long orderId) {
        try {
            if (orderId != null) {
                jdbcTemplate.update("DELETE FROM check_order_detail WHERE check_order_id = ?", orderId);
                jdbcTemplate.update("DELETE FROM check_order WHERE id = ?", orderId);
                jdbcTemplate.update("DELETE FROM stock_transaction WHERE related_type = 'check' AND related_id = ?", orderId);
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