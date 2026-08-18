package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.Warehouse.AvailableOutOrderDto;
import com.tonghui.erp.Common.Dto.Warehouse.OutOrderMaterialDto;
import com.tonghui.erp.Common.Dto.Warehouse.ReturnItemRequest;
import com.tonghui.erp.Common.Dto.Warehouse.ReturnOrderCreateDto;
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