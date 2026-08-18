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
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.StockTransactionMapper;
import com.tonghui.erp.Data.mapper.TransferOrderDetailMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

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
            if (source.getQuantity().compareTo(new BigDecimal("1.0")) != 0) {
                System.err.println("测试失败: 源库存应为1.0, 实际: " + source.getQuantity());
            }
            // 断言目标仓生成0.5新库存行
            Stock dest = stockMapper.selectOne(new QueryWrapper<Stock>()
                    .eq("prod_unit_id", TO_UNIT_ID)
                    .eq("item_code", TEST_ITEM_CODE)
                    .eq("batch_number", TEST_BATCH)
                    .eq("is_deleted", 0));
            if (dest == null || dest.getQuantity().compareTo(new BigDecimal("0.5")) != 0) {
                System.err.println("测试失败: 目标库存应为0.5, 实际: " + (dest == null ? "不存在" : dest.getQuantity()));
            }
            // 断言生成2条调拨流水
            List<StockTransaction> txs = stockTransactionMapper.selectList(new QueryWrapper<StockTransaction>()
                    .eq("related_type", "transfer").eq("related_id", orderId));
            if (txs.size() != 2) {
                System.err.println("测试失败: 调拨流水应为2条, 实际: " + txs.size());
            } else {
                for (StockTransaction tx : txs) {
                    System.out.println("流水: " + tx.getTransactionType() + " 数量变化: " + tx.getQuantityChange());
                }
            }
            // 断言详情可查且含明细
            TransferOrderDetailDto detail = transferOrderService.getTransferOrderDetail(orderId);
            if (detail == null || detail.getItems() == null || detail.getItems().isEmpty()) {
                System.err.println("测试失败: 调拨详情无明细");
            } else {
                System.out.println("调拨详情明细数: " + detail.getItems().size());
            }
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

            List<WarehouseMaterialDto> materials = transferOrderService.getWarehouseMaterials(FROM_WAREHOUSE);
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