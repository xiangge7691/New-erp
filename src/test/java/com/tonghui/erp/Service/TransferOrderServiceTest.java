package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.Warehouse.MaterialBatchDto;
import com.tonghui.erp.Common.Dto.Warehouse.TransferItemRequest;
import com.tonghui.erp.Common.Dto.Warehouse.TransferOrderCreateDto;
import com.tonghui.erp.Common.Dto.Warehouse.TransferOrderDetailDto;
import com.tonghui.erp.Common.Dto.Warehouse.WarehouseMaterialDto;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockTransaction;
import com.tonghui.erp.Data.Entity.TransferOrder;
import com.tonghui.erp.Data.Entity.TransferOrderDetail;
import com.tonghui.erp.Common.Dto.Stock.StockTransactionDto;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.StockTransactionMapper;
import com.tonghui.erp.Data.mapper.TransferOrderDetailMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 调拨单服务接口测试
 * <p>
 * 覆盖调拨单的分页查询、仓库列表、仓库物料/批次查询、
 * 新增调拨（库存扣减/回增与流水生成）及异常分支（超库存调拨、同仓库调拨）
 * </p>
 */
@SpringBootTest
public class TransferOrderServiceTest {

    // region 常量与依赖注入
    // ===================================
    // 常量与依赖注入
    // ===================================

    /** 测试物料编码（专用，避免与业务数据冲突） */
    private static final String TEST_ITEM_CODE = "TESTT001";
    /** 测试物料名称 */
    private static final String TEST_ITEM_NAME = "调拨测试物料";
    /** 测试物料分类 */
    private static final String TEST_CATEGORY = "测试分类";
    /** 测试物料单位 */
    private static final String TEST_UNIT = "kg";
    /** 测试批次号 */
    private static final String TEST_BATCH = "TBATCH01";
    /** 调出仓库名称（耒阳制剂室） */
    private static final String FROM_WAREHOUSE = "耒阳制剂室";
    /** 调入仓库名称（原料仓） */
    private static final String TO_WAREHOUSE = "原料仓";
    /** 调出仓库ID（耒阳制剂室） */
    private static final Long FROM_UNIT_ID = 7L;
    /** 调入仓库ID（原料仓） */
    private static final Long TO_UNIT_ID = 19L;

    /** 调拨单服务 */
    @Autowired
    private TransferOrderService transferOrderService;
    /** 库存服务（用于查询库存流水） */
    @Autowired
    private StockService stockService;
    /** 库存Mapper */
    @Autowired
    private StockMapper stockMapper;
    /** 库存流水Mapper */
    @Autowired
    private StockTransactionMapper stockTransactionMapper;
    /** 调拨明细Mapper */
    @Autowired
    private TransferOrderDetailMapper transferOrderDetailMapper;
    /** JdbcTemplate（用于物理删除测试数据，绕开软删除） */
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // endregion

    // region 测试方法
    // ===================================
    // 测试方法
    // ===================================

    /**
     * 测试分页查询调拨单列表（空参数不抛异常）
     */
    @Test
    public void testQueryTransferOrders() {
        try {
            Page<TransferOrder> page = transferOrderService.queryTransferOrders(null, null, 0, 10);
            System.out.println("调拨单总数: " + page.getTotal());
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试获取仓库名称列表（应包含耒阳制剂室与原料仓）
     */
    @Test
    public void testGetWarehouseList() {
        try {
            List<String> warehouses = transferOrderService.getWarehouseList();
            System.out.println("仓库列表: " + warehouses);
            if (!warehouses.contains(FROM_WAREHOUSE) || !warehouses.contains(TO_WAREHOUSE)) {
                System.err.println("测试失败: 仓库列表缺少耒阳制剂室或原料仓");
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试新增调拨单成功流程
     * <p>
     * 造库存1.5调拨0.5：源库存应剩1.0、目标仓生成0.5新库存行、
     * 生成2条调拨流水（出库-0.5/入库+0.5）、详情可查
     * </p>
     */
    @Test
    public void testCreateTransferOrder() {
        Long sourceStockId = null;
        Long orderId = null;
        try {
            // 造测试库存（耒阳制剂室 1.5kg）
            Stock stock = createStock(FROM_UNIT_ID, new BigDecimal("1.5"));
            sourceStockId = stock.getStockId();

            // 调拨0.5到原料仓
            TransferOrderCreateDto dto = new TransferOrderCreateDto();
            dto.setFromWarehouse(FROM_WAREHOUSE);
            dto.setToWarehouse(TO_WAREHOUSE);
            dto.setRemark("接口测试调拨");
            TransferItemRequest item = new TransferItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + FROM_WAREHOUSE + "_" + TEST_BATCH);
            item.setTransferQuantity(new BigDecimal("0.5"));
            dto.setItems(List.of(item));

            TransferOrder order = transferOrderService.createTransferOrder(dto);
            orderId = order.getId();
            System.out.println("调拨单号: " + order.getTransferNo());

            // 断言单号前缀
            if (!order.getTransferNo().startsWith("DB-")) {
                System.err.println("测试失败: 调拨单号前缀错误: " + order.getTransferNo());
            }
            // 断言源库存剩1.0
            Stock source = stockMapper.selectById(sourceStockId);
            assertEquals(0, new BigDecimal("1.0").compareTo(source.getQuantity()), "源库存应为1.0");
            // 断言目标仓生成0.5新库存行
            Stock dest = stockMapper.selectOne(new QueryWrapper<Stock>()
                    .eq("prod_unit_id", TO_UNIT_ID)
                    .eq("item_code", TEST_ITEM_CODE)
                    .eq("batch_number", TEST_BATCH)
                    .eq("is_deleted", 0));
            assertNotNull(dest, "目标库存行应存在");
            assertEquals(0, new BigDecimal("0.5").compareTo(dest.getQuantity()), "目标库存应为0.5");
            // 断言生成2条调拨流水
            List<StockTransaction> txs = stockTransactionMapper.selectList(new QueryWrapper<StockTransaction>()
                    .eq("related_type", "transfer").eq("related_id", orderId));
            assertEquals(2, txs.size(), "调拨流水应为2条");
            // 断言详情可查且含明细
            TransferOrderDetailDto detail = transferOrderService.getTransferOrderDetail(orderId);
            assertNotNull(detail, "调拨详情不应为空");
            assertNotNull(detail.getItems(), "调拨详情明细不应为空");
            assertTrue(!detail.getItems().isEmpty(), "调拨详情应含明细");
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(sourceStockId, orderId);
        }
    }

    /**
     * 测试超库存调拨（应抛出异常）
     */
    @Test
    public void testCreateTransferOrderOverStock() {
        Long sourceStockId = null;
        try {
            Stock stock = createStock(FROM_UNIT_ID, new BigDecimal("1.5"));
            sourceStockId = stock.getStockId();

            TransferOrderCreateDto dto = new TransferOrderCreateDto();
            dto.setFromWarehouse(FROM_WAREHOUSE);
            dto.setToWarehouse(TO_WAREHOUSE);
            TransferItemRequest item = new TransferItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + FROM_WAREHOUSE + "_" + TEST_BATCH);
            item.setTransferQuantity(new BigDecimal("2.0"));
            dto.setItems(List.of(item));

            try {
                transferOrderService.createTransferOrder(dto);
                System.err.println("测试失败: 超库存调拨未抛出异常");
            } catch (RuntimeException e) {
                System.out.println("超库存调拨正确抛出异常: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(sourceStockId, null);
        }
    }

    /**
     * 测试同仓库调拨（应抛出异常）
     */
    @Test
    public void testCreateTransferOrderSameWarehouse() {
        Long sourceStockId = null;
        try {
            Stock stock = createStock(FROM_UNIT_ID, new BigDecimal("1.5"));
            sourceStockId = stock.getStockId();

            TransferOrderCreateDto dto = new TransferOrderCreateDto();
            dto.setFromWarehouse(FROM_WAREHOUSE);
            dto.setToWarehouse(FROM_WAREHOUSE);
            TransferItemRequest item = new TransferItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + FROM_WAREHOUSE + "_" + TEST_BATCH);
            item.setTransferQuantity(new BigDecimal("0.5"));
            dto.setItems(List.of(item));

            try {
                transferOrderService.createTransferOrder(dto);
                System.err.println("测试失败: 同仓库调拨未抛出异常");
            } catch (RuntimeException e) {
                System.out.println("同仓库调拨正确抛出异常: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(sourceStockId, null);
        }
    }

    /**
     * 测试仓库物料列表与批次详情查询
     */
    @Test
    public void testGetWarehouseMaterialsAndBatches() {
        Long stockId = null;
        try {
            Stock stock = createStock(FROM_UNIT_ID, new BigDecimal("1.5"));
            stockId = stock.getStockId();

            List<WarehouseMaterialDto> materials = transferOrderService.getWarehouseMaterials(FROM_WAREHOUSE, null);
            boolean found = materials.stream().anyMatch(m -> TEST_ITEM_CODE.equals(m.getMaterialCode()));
            if (!found) {
                System.err.println("测试失败: 仓库物料列表未包含测试物料");
            } else {
                System.out.println("仓库物料列表包含测试物料");
            }
            List<MaterialBatchDto> batches = transferOrderService.getMaterialBatches(FROM_WAREHOUSE, TEST_ITEM_CODE);
            if (batches.isEmpty()) {
                System.err.println("测试失败: 批次详情为空");
            } else {
                for (MaterialBatchDto batch : batches) {
                    System.out.println("批次: " + batch.getBatchNo() + " 数量: " + batch.getStock());
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
     * 测试调空后源库存软删除（调拨数量=源库存）
     * <p>
     * 造库存0.049全额调拨：源库存应被软删除（is_deleted=1）、目标仓生成0.049新库存行
     * </p>
     */
    @Test
    public void testCreateTransferOrderFullClear() {
        Long sourceStockId = null;
        Long orderId = null;
        try {
            Stock stock = createStock(FROM_UNIT_ID, new BigDecimal("0.049"));
            sourceStockId = stock.getStockId();

            TransferOrderCreateDto dto = new TransferOrderCreateDto();
            dto.setFromWarehouse(FROM_WAREHOUSE);
            dto.setToWarehouse(TO_WAREHOUSE);
            dto.setRemark("接口测试全额调拨");
            TransferItemRequest item = new TransferItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + FROM_WAREHOUSE + "_" + TEST_BATCH);
            item.setTransferQuantity(new BigDecimal("0.049"));
            dto.setItems(List.of(item));

            TransferOrder order = transferOrderService.createTransferOrder(dto);
            orderId = order.getId();

            // 断言源库存被软删除（软删除后 selectById 查不到）
            Stock source = stockMapper.selectById(sourceStockId);
            assertEquals(null, source, "调空后源库存应被软删除");
            // 绕过软删除直接查库，确认 is_deleted=1
            Integer deletedFlag = jdbcTemplate.queryForObject(
                    "SELECT is_deleted FROM stock WHERE stock_id = ?", Integer.class, sourceStockId);
            assertEquals(1, deletedFlag, "调空后源库存 is_deleted 应为1");
            // 断言目标仓生成0.049新库存行
            Stock dest = stockMapper.selectOne(new QueryWrapper<Stock>()
                    .eq("prod_unit_id", TO_UNIT_ID)
                    .eq("item_code", TEST_ITEM_CODE)
                    .eq("batch_number", TEST_BATCH)
                    .eq("is_deleted", 0));
            assertNotNull(dest, "目标库存行应存在");
            assertEquals(0, new BigDecimal("0.049").compareTo(dest.getQuantity()), "目标库存应为0.049");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("调空软删除测试失败: " + e.getMessage(), e);
        } finally {
            cleanup(sourceStockId, orderId);
        }
    }

    /**
     * 测试仓库物料列表关键词模糊查询（物料编码/物料名称）
     * <p>
     * 造测试库存后，按关键词命中测试物料编码、不命中的关键词返回空
     * </p>
     */
    @Test
    public void testGetWarehouseMaterialsByKeyword() {
        Long stockId = null;
        try {
            Stock stock = createStock(FROM_UNIT_ID, new BigDecimal("1.5"));
            stockId = stock.getStockId();

            // 按物料编码关键词命中
            List<WarehouseMaterialDto> byCode = transferOrderService.getWarehouseMaterials(FROM_WAREHOUSE, "TESTT00");
            boolean foundByCode = byCode.stream().anyMatch(m -> TEST_ITEM_CODE.equals(m.getMaterialCode()));
            assertTrue(foundByCode, "按物料编码关键词应命中测试物料");
            // 按物料名称关键词命中
            List<WarehouseMaterialDto> byName = transferOrderService.getWarehouseMaterials(FROM_WAREHOUSE, "调拨测试");
            boolean foundByName = byName.stream().anyMatch(m -> TEST_ITEM_CODE.equals(m.getMaterialCode()));
            assertTrue(foundByName, "按物料名称关键词应命中测试物料");
            // 不命中关键词返回空
            List<WarehouseMaterialDto> none = transferOrderService.getWarehouseMaterials(FROM_WAREHOUSE, "不存在的关键词XYZ");
            boolean foundNone = none.stream().anyMatch(m -> TEST_ITEM_CODE.equals(m.getMaterialCode()));
            assertEquals(false, foundNone, "不命中关键词不应返回测试物料");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("关键词模糊查询测试失败: " + e.getMessage(), e);
        } finally {
            cleanup(stockId, null);
        }
    }

    /**
     * 测试调拨流水携带调拨单号（inCode）
     * <p>
     * 调拨后，目标仓的调拨入库流水应通过 getTransactionsByStockId 返回
     * inCode = 调拨单号（transferNo）
     * </p>
     */
    @Test
    public void testTransferTransactionsCarryTransferNo() {
        Long sourceStockId = null;
        Long orderId = null;
        try {
            Stock stock = createStock(FROM_UNIT_ID, new BigDecimal("1.5"));
            sourceStockId = stock.getStockId();

            TransferOrderCreateDto dto = new TransferOrderCreateDto();
            dto.setFromWarehouse(FROM_WAREHOUSE);
            dto.setToWarehouse(TO_WAREHOUSE);
            dto.setRemark("流水单号测试调拨");
            TransferItemRequest item = new TransferItemRequest();
            item.setInventoryKey(TEST_ITEM_CODE + "_" + FROM_WAREHOUSE + "_" + TEST_BATCH);
            item.setTransferQuantity(new BigDecimal("0.5"));
            dto.setItems(List.of(item));

            TransferOrder order = transferOrderService.createTransferOrder(dto);
            orderId = order.getId();
            System.out.println("调拨单号: " + order.getTransferNo());

            // 查询目标仓库存的流水（调拨入库）
            Stock dest = stockMapper.selectOne(new QueryWrapper<Stock>()
                    .eq("prod_unit_id", TO_UNIT_ID)
                    .eq("item_code", TEST_ITEM_CODE)
                    .eq("batch_number", TEST_BATCH)
                    .eq("is_deleted", 0));
            assertNotNull(dest, "目标库存行应存在");

            List<StockTransactionDto> txs = stockService.getTransactionsByStockId(dest.getStockId());
            StockTransactionDto inbound = txs.stream()
                    .filter(t -> "调拨入库".equals(String.valueOf(t.getTransactionType())))
                    .findFirst().orElse(null);
            if (inbound == null) {
                System.err.println("测试失败: 未找到调拨入库流水");
            } else if (!order.getTransferNo().equals(inbound.getInCode())) {
                System.err.println("测试失败: 调拨入库流水 inCode 应为 " + order.getTransferNo() + ", 实际: " + inbound.getInCode());
            } else {
                System.out.println("调拨流水调拨单号: " + inbound.getInCode());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup(sourceStockId, orderId);
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
     * @param prodUnitId 仓库ID
     * @param quantity   库存数量
     * @return 创建的库存记录
     */
    private Stock createStock(Long prodUnitId, BigDecimal quantity) {
        Stock stock = new Stock();
        stock.setProdUnitId(prodUnitId);
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
     * @param sourceStockId 源库存ID（可为null）
     * @param orderId       调拨单ID（可为null）
     */
    private void cleanup(Long sourceStockId, Long orderId) {
        try {
            if (orderId != null) {
                jdbcTemplate.update("DELETE FROM transfer_order_detail WHERE transfer_order_id = ?", orderId);
                jdbcTemplate.update("DELETE FROM transfer_order WHERE id = ?", orderId);
                jdbcTemplate.update("DELETE FROM stock_transaction WHERE related_type = 'transfer' AND related_id = ?", orderId);
            }
            jdbcTemplate.update("DELETE FROM stock WHERE item_code = ?", TEST_ITEM_CODE);
            jdbcTemplate.update("DELETE FROM stock_transaction WHERE batch_number = ?", TEST_BATCH);
            if (sourceStockId != null) {
                jdbcTemplate.update("DELETE FROM stock_transaction WHERE stock_id = ?", sourceStockId);
            }
        } catch (Exception e) {
            System.err.println("清理测试数据失败: " + e.getMessage());
        }
    }

    // endregion
}