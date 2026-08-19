package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Common.Dto.Stock.StockTransactionDto;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存联动集成测试
 * <p>
 * 覆盖入库确认/取消、出库确认/取消、库存不足回滚等库存联动核心逻辑
 * （对应原数据库触发器的逻辑迁移验证），业务数据事务回滚不污染数据库
 * </p>
 */
@SpringBootTest
public class InventoryFlowTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 入库单服务
     */
    @Autowired
    private StockInService stockInService;

    /**
     * 出库单服务
     */
    @Autowired
    private StockOutService stockOutService;

    /**
     * 库存服务
     */
    @Autowired
    private StockService stockService;

    /**
     * 库存数据访问层
     */
    @Autowired
    private StockMapper stockMapper;

    /**
     * 生产单位数据访问层
     */
    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    /**
     * 数据源（用于执行幂等建表脚本）
     */
    @Autowired
    private DataSource dataSource;

    // endregion

    // region 测试初始化
    // ===================================
    // 测试初始化
    // ===================================

    /**
     * 建表标志位（只执行一次幂等建表脚本）
     */
    private static boolean tablesInitialized = false;

    /**
     * 每个测试前确保验收/库存相关表结构就绪（幂等）
     */
    @BeforeEach
    public void ensureTables() {
        if (tablesInitialized) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new FileSystemResource("sql/acceptance_create_tables.sql"));
            tablesInitialized = true;
        } catch (Exception e) {
            throw new RuntimeException("初始化表结构失败: " + e.getMessage(), e);
        }
    }

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试添加入库单直接生效与取消回滚：添加即入库（库存增加+状态已入库+写流水）→ 取消（库存回滚+写调整流水）
     */
    @Test
    @Transactional
    public void testAddStockInDirectlyEffectiveAndCancel() {
        Long prodUnitId = findAnyProdUnitId();
        if (prodUnitId == null) {
            System.out.println("无生产单位数据，跳过该测试");
            return;
        }

        // 创建入库单（含一条明细），添加后直接生效入库
        StockIn stockIn = new StockIn();
        stockIn.setInType("采购入库");
        stockIn.setProdUnitId(prodUnitId);
        stockIn.setInDate(LocalDateTime.now());
        StockInDetail detail = new StockInDetail();
        detail.setItemType("material");
        detail.setItemCode("INV-TEST-001");
        detail.setItemName("联动测试物料");
        detail.setCategoryName("原料");
        detail.setUnitName("kg");
        detail.setBatchNumber("INV-BT-001");
        detail.setQuantity(new BigDecimal("5.000"));
        detail.setUnitPrice(new BigDecimal("10.00"));
        detail.setStockStatus("合格");
        stockInService.addStockIn(stockIn, List.of(detail));
        Long inId = stockIn.getInId();

        // 添加即生效：直接断言库存增加 + 写流水 + 状态已入库
        assertEquals("已入库", stockInService.getStockInById(inId).getInStatus(), "添加后入库单应为已入库状态");
        Stock stock = findStock("INV-TEST-001", prodUnitId, "INV-BT-001");
        assertNotNull(stock, "添加入库后库存批次应存在");
        assertEquals(0, new BigDecimal("5.000").compareTo(stock.getQuantity()), "库存数量应为5");
        List<StockTransactionDto> transactions = stockService.getTransactionsByStockId(stock.getStockId());
        assertEquals(1, transactions.size(), "添加入库后应有1条流水");
        assertEquals("采购入库", String.valueOf(transactions.get(0).getTransactionType()), "流水类型应为采购入库");

        // 取消入库 → 库存回滚
        stockInService.cancelStockIn(inId);
        Stock afterCancel = stockMapper.selectById(stock.getStockId());
        assertNull(afterCancel, "取消入库后库存批次应被删除（数量归零）");
        List<StockTransactionDto> afterCancelTxs = stockService.getTransactionsByStockId(stock.getStockId());
        assertEquals(2, afterCancelTxs.size(), "取消入库后应有2条流水（入库+调整）");
        assertTrue(afterCancelTxs.stream().anyMatch(t -> "调整".equals(String.valueOf(t.getTransactionType()))),
                "取消入库后应包含调整类型流水");
    }

    /**
     * 测试添加出库单直接生效与取消恢复：添加即出库（库存扣减+写流水）→ 取消（库存恢复）
     */
    @Test
    @Transactional
    public void testAddStockOutDirectlyEffectiveAndCancel() {
        Long prodUnitId = findAnyProdUnitId();
        if (prodUnitId == null) {
            System.out.println("无生产单位数据，跳过该测试");
            return;
        }

        // 先入库建立库存（数量10）
        Stock stock = buildStock("INV-TEST-002", prodUnitId, "INV-BT-002", new BigDecimal("10.000"));
        Long stockId = stock.getStockId();

        // 创建出库单（出库数量4），添加后直接生效出库
        StockOut stockOut = new StockOut();
        stockOut.setOutType("生产领料出库");
        stockOut.setProdUnitId(prodUnitId);
        stockOut.setOutDate(LocalDateTime.now());
        StockOutDetail outDetail = new StockOutDetail();
        outDetail.setStockId(stockId);
        outDetail.setItemCode("INV-TEST-002");
        outDetail.setItemName("联动测试物料2");
        outDetail.setCategoryName("原料");
        outDetail.setUnitName("kg");
        outDetail.setBatchNumber("INV-BT-002");
        outDetail.setQuantity(new BigDecimal("4.000"));
        stockOutService.addStockOut(stockOut, List.of(outDetail));
        Long outId = stockOut.getOutId();

        // 添加即生效：直接断言库存扣减 + 写流水 + 状态已出库
        assertEquals("已出库", stockOutService.getStockOutById(outId).getOutStatus(), "添加后出库单应为已出库状态");
        Stock afterConfirm = stockMapper.selectById(stockId);
        assertNotNull(afterConfirm, "出库后库存批次应存在");
        assertEquals(0, new BigDecimal("6.000").compareTo(afterConfirm.getQuantity()), "出库后库存应为6");
        List<StockTransactionDto> transactions = stockService.getTransactionsByStockId(stockId);
        assertEquals(1, transactions.size(), "出库后应有1条流水");
        assertTrue(new BigDecimal("-4.000").compareTo(
                (BigDecimal) transactions.get(0).getQuantityChange()) == 0, "流水数量变化应为-4");

        // 取消出库 → 库存恢复
        stockOutService.cancelStockOut(outId);
        Stock afterCancel = stockMapper.selectById(stockId);
        assertNotNull(afterCancel, "取消出库后库存批次应存在");
        assertEquals(0, new BigDecimal("10.000").compareTo(afterCancel.getQuantity()), "取消出库后库存应恢复为10");
        List<StockTransactionDto> afterCancelTxs = stockService.getTransactionsByStockId(stockId);
        assertEquals(2, afterCancelTxs.size(), "取消出库后应有2条流水（出库+调整）");
        assertTrue(afterCancelTxs.stream().anyMatch(t -> "调整".equals(String.valueOf(t.getTransactionType()))),
                "取消出库后应包含调整类型流水");
    }

    /**
     * 测试出库库存不足时整体回滚（原子性）：
     * 添加出库单直接抛"库存不足"异常，且出库单、明细、库存均不落库
     * <p>不使用测试事务，直接验证 Service 独立事务的回滚效果，结尾手动清理测试数据</p>
     */
    @Test
    public void testOutboundInsufficientStockRollback() {
        Long prodUnitId = findAnyProdUnitId();
        if (prodUnitId == null) {
            System.out.println("无生产单位数据，跳过该测试");
            return;
        }

        // 建立库存（数量3）
        Stock stock = buildStock("INV-TEST-003", prodUnitId, "INV-BT-003", new BigDecimal("3.000"));
        try {
            // 创建出库单（出库数量100，远超库存），添加时应直接抛库存不足异常
            StockOut stockOut = new StockOut();
            stockOut.setOutType("销售出库");
            stockOut.setProdUnitId(prodUnitId);
            stockOut.setOutDate(LocalDateTime.now());
            StockOutDetail outDetail = new StockOutDetail();
            outDetail.setStockId(stock.getStockId());
            outDetail.setItemCode("INV-TEST-003");
            outDetail.setItemName("联动测试物料3");
            outDetail.setCategoryName("原料");
            outDetail.setUnitName("kg");
            outDetail.setBatchNumber("INV-BT-003");
            outDetail.setQuantity(new BigDecimal("100.000"));
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> stockOutService.addStockOut(stockOut, List.of(outDetail)));
            assertTrue(ex.getMessage().contains("库存不足"), "异常信息应提示库存不足");

            // 原子性校验：库存保持不变 + 出库单未落库
            Stock after = stockMapper.selectById(stock.getStockId());
            assertNotNull(after);
            assertEquals(0, new BigDecimal("3.000").compareTo(after.getQuantity()), "库存不足时库存应保持不变");
            assertNull(stockOutService.getStockOutByCode(stockOut.getOutCode()), "库存不足时出库单据应整体回滚");
        } finally {
            // 清理测试数据（出库单已回滚无需清理，仅清理直插的库存批次）
            stockMapper.deleteById(stock.getStockId());
        }
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 查询任意一个生产单位ID（仓库），无数据时返回null
     *
     * @return 生产单位ID
     */
    private Long findAnyProdUnitId() {
        try {
            List<ProductionUnit> units = productionUnitMapper.selectList(null);
            return units.isEmpty() ? null : units.get(0).getProdUnitId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 按 物料编码+仓库+批号 查询库存批次
     */
    private Stock findStock(String itemCode, Long prodUnitId, String batchNumber) {
        return stockMapper.selectOne(new QueryWrapper<Stock>()
                .eq("item_code", itemCode)
                .eq("prod_unit_id", prodUnitId)
                .eq("batch_number", batchNumber));
    }

    /**
     * 直接插入一个库存批次（供出库测试使用）
     */
    private Stock buildStock(String itemCode, Long prodUnitId, String batchNumber, BigDecimal quantity) {
        Stock stock = new Stock();
        stock.setProdUnitId(prodUnitId);
        stock.setItemType("material");
        stock.setItemCode(itemCode);
        stock.setItemName("联动测试物料");
        stock.setCategoryName("原料");
        stock.setUnitName("kg");
        stock.setBatchNumber(batchNumber);
        stock.setQuantity(quantity);
        stock.setUnitPrice(new BigDecimal("10.00"));
        stock.setStockStatus("合格");
        stockMapper.insert(stock);
        return stock;
    }

    // endregion
}
