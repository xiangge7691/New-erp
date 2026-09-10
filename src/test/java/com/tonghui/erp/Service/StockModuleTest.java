package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningDTO;
import com.tonghui.erp.Common.Dto.Stock.ExpiryWarningStatsDTO;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.StockInDetail;
import com.tonghui.erp.Data.Entity.StockOut;
import com.tonghui.erp.Data.Entity.StockOutDetail;
import com.tonghui.erp.Common.Dto.Stock.StockTransactionDto;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存域综合测试
 * <p>
 * 覆盖库存查询、入库确认、出库确认、有效期预警、库存流水等核心业务逻辑，
 * 全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
public class StockModuleTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 库存服务
     */
    @Autowired
    private StockService stockService;

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
     * 库存数据访问层
     */
    @Autowired
    private StockMapper stockMapper;

    /**
     * 生产单位数据访问层
     */
    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试入库单新增即入库：库存增加、状态已入库、生成流水
     */
    @Test
    @Transactional
    public void testAddStockInAndQuery() {
        Long prodUnitId = findAnyProdUnitId();
        if (prodUnitId == null) {
            System.out.println("无生产单位数据，跳过该测试");
            return;
        }

        String itemCode = "STK-IN-" + System.currentTimeMillis();
        String batch = "BT-" + System.currentTimeMillis();

        StockIn stockIn = new StockIn();
        stockIn.setInType("采购入库");
        stockIn.setProdUnitId(prodUnitId);
        stockIn.setInDate(LocalDateTime.now());
        StockInDetail detail = new StockInDetail();
        detail.setItemType("material");
        detail.setItemCode(itemCode);
        detail.setItemName("库存域测试物料");
        detail.setCategoryName("原料");
        detail.setUnitName("kg");
        detail.setBatchNumber(batch);
        detail.setQuantity(new BigDecimal("8.000"));
        detail.setUnitPrice(new BigDecimal("10.00"));
        detail.setStockStatus("合格");
        stockInService.addStockIn(stockIn, List.of(detail));
        Long inId = stockIn.getInId();

        // 添加即生效
        assertEquals("已入库", stockInService.getStockInById(inId).getInStatus(), "入库单应为已入库状态");
        assertNotNull(stockInService.getStockInByCode(stockIn.getInCode()), "按入库单号应可查询");

        // 库存已建立
        Stock stock = findStock(itemCode, prodUnitId, batch);
        assertNotNull(stock, "库存批次应已建立");
        assertEquals(0, new BigDecimal("8.000").compareTo(stock.getQuantity()), "库存数量应为8");

        // 流水已生成
        List<StockTransactionDto> transactions = stockService.getTransactionsByStockId(stock.getStockId());
        assertEquals(1, transactions.size(), "应生成1条入库流水");

        // 出库单号生成
        assertNotNull(stockOutService.generateStockOutCode(), "出库单号生成不应为空");
    }

    /**
     * 测试出库扣减库存与流水：先入库再出库，库存扣减并生成出库流水
     */
    @Test
    @Transactional
    public void testAddStockOutDeduct() {
        Long prodUnitId = findAnyProdUnitId();
        if (prodUnitId == null) {
            System.out.println("无生产单位数据，跳过该测试");
            return;
        }

        String itemCode = "STK-OUT-" + System.currentTimeMillis();
        String batch = "BT-OUT-" + System.currentTimeMillis();

        // 直接建库存 20
        Stock stock = buildStock(itemCode, prodUnitId, batch, new BigDecimal("20.000"));
        Long stockId = stock.getStockId();

        // 出库 6
        StockOut stockOut = new StockOut();
        stockOut.setOutType("生产领料出库");
        stockOut.setProdUnitId(prodUnitId);
        stockOut.setOutDate(LocalDateTime.now());
        StockOutDetail outDetail = new StockOutDetail();
        outDetail.setStockId(stockId);
        outDetail.setItemCode(itemCode);
        outDetail.setItemName("库存域测试物料");
        outDetail.setCategoryName("原料");
        outDetail.setUnitName("kg");
        outDetail.setBatchNumber(batch);
        outDetail.setQuantity(new BigDecimal("6.000"));
        stockOutService.addStockOut(stockOut, List.of(outDetail));
        Long outId = stockOut.getOutId();

        // 添加即生效
        assertEquals("已出库", stockOutService.getStockOutById(outId).getOutStatus(), "出库单应为已出库状态");
        Stock after = stockMapper.selectById(stockId);
        assertNotNull(after, "出库后库存批次应存在");
        assertEquals(0, new BigDecimal("14.000").compareTo(after.getQuantity()), "出库后库存应为14");

        // 出库流水
        List<StockTransactionDto> transactions = stockService.getTransactionsByStockId(stockId);
        assertEquals(1, transactions.size(), "应生成1条出库流水");
        assertEquals(0, new BigDecimal("-6.000").compareTo((BigDecimal) transactions.get(0).getQuantityChange()),
                "流水数量变化应为-6");
    }

    /**
     * 测试出库库存不足抛异常且事务回滚：库存3出库100应报"库存不足"，单据不落库
     */
    @Test
    public void testStockOutInsufficientRollback() {
        Long prodUnitId = findAnyProdUnitId();
        if (prodUnitId == null) {
            System.out.println("无生产单位数据，跳过该测试");
            return;
        }
        Stock stock = buildStock("STK-FAIL-" + System.currentTimeMillis(), prodUnitId,
                "BT-FAIL-" + System.currentTimeMillis(), new BigDecimal("3.000"));
        try {
            StockOut stockOut = new StockOut();
            stockOut.setOutType("销售出库");
            stockOut.setProdUnitId(prodUnitId);
            stockOut.setOutDate(LocalDateTime.now());
            StockOutDetail outDetail = new StockOutDetail();
            outDetail.setStockId(stock.getStockId());
            outDetail.setItemCode(stock.getItemCode());
            outDetail.setItemName(stock.getItemName());
            outDetail.setCategoryName("原料");
            outDetail.setUnitName("kg");
            outDetail.setBatchNumber(stock.getBatchNumber());
            outDetail.setQuantity(new BigDecimal("100.000"));
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> stockOutService.addStockOut(stockOut, List.of(outDetail)));
            assertTrue(ex.getMessage().contains("库存不足"), "异常信息应提示库存不足");

            Stock after = stockMapper.selectById(stock.getStockId());
            assertNotNull(after, "库存批次应仍存在");
            assertEquals(0, new BigDecimal("3.000").compareTo(after.getQuantity()), "库存应保持不变");
            assertNull(stockOutService.getStockOutByCode(stockOut.getOutCode()), "出库单据应整体回滚");
        } finally {
            stockMapper.deleteById(stock.getStockId());
        }
    }

    /**
     * 测试库存查询与分组搜索：入库后按编码搜索可见、分组统计正确
     */
    @Test
    @Transactional
    public void testStockQueryAndGrouped() {
        Long prodUnitId = findAnyProdUnitId();
        if (prodUnitId == null) {
            System.out.println("无生产单位数据，跳过该测试");
            return;
        }

        String itemCode = "STK-QRY-" + System.currentTimeMillis();
        Stock stock = buildStock(itemCode, prodUnitId, "BT-QRY-1", new BigDecimal("5.000"));

        // 按编码查询
        Stock query = new Stock();
        query.setItemCode(itemCode);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Stock> page =
                stockService.queryStocks(query, null, 1, 10);
        assertEquals(1, page.getTotal(), "按编码查询应命中1条库存");

        // 分组搜索（分页参数为0基页码）
        com.tonghui.erp.Common.Dto.Stock.StockGroupedDto grouped = stockService.groupedSearch(
                itemCode, null, null, prodUnitId, null, false, 0, 10).getItems().stream()
                .filter(g -> itemCode.equals(g.getItemCode())).findFirst().orElse(null);
        assertNotNull(grouped, "分组搜索应命中该物料");
        assertEquals(1, grouped.getBatchCount(), "批次数应为1");
        assertEquals(0, new BigDecimal("5.000").compareTo(grouped.getTotalQuantity()), "分组总数应为5");
    }

    /**
     * 测试有效期预警：临近效期物料进入预警列表、统计口径正确
     */
    @Test
    @Transactional
    public void testExpiryWarning() {
        Long prodUnitId = findAnyProdUnitId();
        if (prodUnitId == null) {
            System.out.println("无生产单位数据，跳过该测试");
            return;
        }

        String itemCode = "STK-EXP-" + System.currentTimeMillis();
        // 通过入库单建立带效期的库存（效期10天后到期，在30天预警范围内）
        StockIn stockIn = new StockIn();
        stockIn.setInType("采购入库");
        stockIn.setProdUnitId(prodUnitId);
        stockIn.setInDate(LocalDateTime.now());
        StockInDetail detail = new StockInDetail();
        detail.setItemType("material");
        detail.setItemCode(itemCode);
        detail.setItemName("预警测试物料");
        detail.setCategoryName("原料");
        detail.setUnitName("kg");
        detail.setBatchNumber("BT-EXP-1");
        detail.setQuantity(new BigDecimal("2.000"));
        detail.setUnitPrice(new BigDecimal("10.00"));
        detail.setStockStatus("合格");
        detail.setExpiryDate(LocalDate.now().plusDays(10));
        stockInService.addStockIn(stockIn, List.of(detail));

        // 预警列表应包含
        List<ExpiryWarningDTO> warnings = stockService.getExpiringStocks(30);
        assertTrue(warnings.stream().anyMatch(w -> itemCode.equals(w.getItemCode())),
                "有效期预警列表应包含临近效期物料");

        // 分页预警查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ExpiryWarningDTO> page =
                stockService.queryExpiringStocks(30, null, null, null, 1, 10);
        assertTrue(page.getTotal() >= 1, "分页预警查询应有数据");

        // 预警统计
        ExpiryWarningStatsDTO stats = stockService.getExpiryWarningStats();
        assertNotNull(stats, "预警统计不应为空");
        assertTrue(stats.getTotalCount() >= 1, "预警总数应>=1");
    }

    /**
     * 测试入库单更新与取消：更新明细数量后取消入库，库存回滚
     */
    @Test
    @Transactional
    public void testStockInUpdateAndCancel() {
        Long prodUnitId = findAnyProdUnitId();
        if (prodUnitId == null) {
            System.out.println("无生产单位数据，跳过该测试");
            return;
        }

        String itemCode = "STK-UPD-" + System.currentTimeMillis();
        String batch = "BT-UPD-" + System.currentTimeMillis();

        StockIn stockIn = new StockIn();
        stockIn.setInType("采购入库");
        stockIn.setProdUnitId(prodUnitId);
        stockIn.setInDate(LocalDateTime.now());
        StockInDetail detail = new StockInDetail();
        detail.setItemType("material");
        detail.setItemCode(itemCode);
        detail.setItemName("更新测试物料");
        detail.setCategoryName("原料");
        detail.setUnitName("kg");
        detail.setBatchNumber(batch);
        detail.setQuantity(new BigDecimal("5.000"));
        detail.setUnitPrice(new BigDecimal("10.00"));
        detail.setStockStatus("合格");
        stockInService.addStockIn(stockIn, List.of(detail));
        Long inId = stockIn.getInId();

        // 明细查询
        List<StockInDetail> details = stockInService.getStockInDetailsByStockInId(inId);
        assertEquals(1, details.size(), "应存在1条入库明细");

        // 更新明细数量为3（明细更新不联动调整库存数量，库存仍为原入库数量5）
        StockInDetail upd = details.get(0);
        upd.setQuantity(new BigDecimal("3.000"));
        stockInService.updateStockInDetail(upd);
        StockInDetail afterUpdate = stockInService.getStockInDetailsByStockInId(inId).get(0);
        assertEquals(0, new BigDecimal("3.000").compareTo(afterUpdate.getQuantity()), "明细数量应已更新");
        Stock stock = findStock(itemCode, prodUnitId, batch);
        assertNotNull(stock, "库存批次应存在");
        assertEquals(0, new BigDecimal("5.000").compareTo(stock.getQuantity()), "更新明细后库存保持不变");

        // 取消入库：库存按明细数量回滚（5-3=2，批次保留）
        stockInService.cancelStockIn(inId);
        Stock afterCancel = findStock(itemCode, prodUnitId, batch);
        assertNotNull(afterCancel, "取消入库后库存批次应保留");
        assertEquals(0, new BigDecimal("2.000").compareTo(afterCancel.getQuantity()), "取消入库后库存应为2");
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
        stock.setItemName("库存域测试物料");
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
