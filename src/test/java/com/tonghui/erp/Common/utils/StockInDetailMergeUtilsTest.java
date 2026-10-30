package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Data.Entity.StockInDetail;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 入库单明细合并工具类单元测试
 * <p>
 * 覆盖同物料同批号合并、仓库/状态/日期差异不合并、空批号归一化、
 * 物品标识缺失时不误合并等场景，不依赖数据库
 * </p>
 */
public class StockInDetailMergeUtilsTest {

    // region 合并场景
    // ===================================
    // 合并场景
    // ===================================

    /**
     * 同物料+同批号+同仓库+同状态+同日期：合并为一条，数量金额累加，单价保留首行
     */
    @Test
    public void testMergeSameMaterialAndBatch() {
        StockInDetail first = buildDetail("MAT-001", "BT-001", 1L, "合格", "5", "10");
        StockInDetail second = buildDetail("MAT-001", "BT-001", 1L, "合格", "3", "12");

        List<StockInDetail> merged = StockInDetailMergeUtils.merge(List.of(first, second));

        assertEquals(1, merged.size(), "同物料同批号应合并为一条");
        StockInDetail result = merged.get(0);
        assertEquals(0, new BigDecimal("8").compareTo(result.getQuantity()), "数量应累加");
        assertEquals(0, new BigDecimal("86").compareTo(result.getAmount()), "金额应累加（50+36）");
        assertEquals(0, new BigDecimal("10").compareTo(result.getUnitPrice()), "单价应保留首行");
    }

    /**
     * 仓库不同：不合并，避免合并后丢失仓库信息导致库存扣错仓
     */
    @Test
    public void testNotMergeDifferentWarehouse() {
        StockInDetail first = buildDetail("MAT-002", "BT-002", 1L, "合格", "5", "10");
        StockInDetail second = buildDetail("MAT-002", "BT-002", 2L, "合格", "3", "10");

        assertEquals(2, StockInDetailMergeUtils.merge(List.of(first, second)).size(),
                "仓库不同不应合并");
    }

    /**
     * 库存状态不同：不合并，合格与待检必须分开管理
     */
    @Test
    public void testNotMergeDifferentStockStatus() {
        StockInDetail first = buildDetail("MAT-003", "BT-003", 1L, "合格", "5", "10");
        StockInDetail second = buildDetail("MAT-003", "BT-003", 1L, "待检", "3", "10");

        assertEquals(2, StockInDetailMergeUtils.merge(List.of(first, second)).size(),
                "库存状态不同不应合并");
    }

    /**
     * 有效期不同：不合并，避免合并后有效期信息失真
     */
    @Test
    public void testNotMergeDifferentExpiryDate() {
        StockInDetail first = buildDetail("MAT-004", "BT-004", 1L, "合格", "5", "10");
        StockInDetail second = buildDetail("MAT-004", "BT-004", 1L, "合格", "3", "10");
        second.setExpiryDate(first.getExpiryDate().plusDays(1));

        assertEquals(2, StockInDetailMergeUtils.merge(List.of(first, second)).size(),
                "有效期不同不应合并");
    }

    /**
     * 空批号与空白批号归一化后视为同一批号，应合并
     */
    @Test
    public void testMergeBlankBatchNumber() {
        StockInDetail first = buildDetail("MAT-005", null, 1L, "合格", "5", "10");
        StockInDetail second = buildDetail("MAT-005", "   ", 1L, null, "3", "10");
        // 空状态按默认“合格”归一化，应与首行一致
        second.setStockStatus(null);

        List<StockInDetail> merged = StockInDetailMergeUtils.merge(List.of(first, second));

        assertEquals(1, merged.size(), "空批号与空白批号应视为同一批号");
        assertEquals(0, new BigDecimal("8").compareTo(merged.get(0).getQuantity()), "数量应累加");
    }

    /**
     * 物品编码和物品ID均缺失：不做合并，避免误合并无关行
     */
    @Test
    public void testNotMergeWithoutItemIdentity() {
        StockInDetail first = buildDetail(null, "BT-006", 1L, "合格", "5", "10");
        StockInDetail second = buildDetail(null, "BT-006", 1L, "合格", "3", "10");

        assertEquals(2, StockInDetailMergeUtils.merge(List.of(first, second)).size(),
                "物品标识缺失时不应合并");
    }

    /**
     * 合并后保持首次出现顺序，且非重复行原样保留
     */
    @Test
    public void testMergeKeepsOriginalOrder() {
        StockInDetail first = buildDetail("MAT-007", "BT-007", 1L, "合格", "5", "10");
        StockInDetail other = buildDetail("MAT-008", "BT-008", 1L, "合格", "2", "10");
        StockInDetail duplicate = buildDetail("MAT-007", "BT-007", 1L, "合格", "1", "10");

        List<StockInDetail> merged = StockInDetailMergeUtils.merge(List.of(first, other, duplicate));

        assertEquals(2, merged.size(), "应合并为两条");
        assertSame(first, merged.get(0), "首行应保持原顺序");
        assertSame(other, merged.get(1), "非重复行应保持原顺序");
    }

    // endregion

    // region 合并键与空值处理
    // ===================================
    // 合并键与空值处理
    // ===================================

    /**
     * 合并键判等：空值入参安全返回false
     */
    @Test
    public void testIsSameMergeKeyWithNull() {
        StockInDetail detail = buildDetail("MAT-009", "BT-009", 1L, "合格", "1", "10");
        assertFalse(StockInDetailMergeUtils.isSameMergeKey(null, detail));
        assertFalse(StockInDetailMergeUtils.isSameMergeKey(detail, null));
        assertTrue(StockInDetailMergeUtils.isSameMergeKey(detail, detail));
    }

    /**
     * 累计时空值安全：目标数量/金额为null时直接取来源值
     */
    @Test
    public void testAccumulateWithNullValues() {
        StockInDetail target = new StockInDetail();
        StockInDetail source = buildDetail("MAT-010", "BT-010", 1L, "合格", "2", "5");

        StockInDetailMergeUtils.accumulate(target, source);

        assertEquals(0, new BigDecimal("2").compareTo(target.getQuantity()), "空数量应取来源数量");
        assertEquals(0, new BigDecimal("10").compareTo(target.getAmount()), "空金额应取来源金额");
        assertNull(target.getRemark(), "来源无备注时目标备注保持为空");
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 构建入库明细测试对象
     *
     * @param itemCode    物料编码
     * @param batchNumber 批号
     * @param prodUnitId  仓库ID
     * @param stockStatus 库存状态
     * @param quantity    数量
     * @param unitPrice   单价
     * @return 入库明细
     */
    private StockInDetail buildDetail(String itemCode, String batchNumber, Long prodUnitId,
                                      String stockStatus, String quantity, String unitPrice) {
        StockInDetail detail = new StockInDetail();
        detail.setItemType("material");
        detail.setItemCode(itemCode);
        detail.setItemName("测试物料");
        detail.setCategoryName("原料");
        detail.setUnitName("kg");
        detail.setBatchNumber(batchNumber);
        detail.setProdUnitId(prodUnitId);
        detail.setStockStatus(stockStatus);
        detail.setQuantity(new BigDecimal(quantity));
        detail.setUnitPrice(new BigDecimal(unitPrice));
        detail.setProductionDate(LocalDate.of(2026, 1, 1));
        detail.setExpiryDate(LocalDate.of(2027, 1, 1));
        detail.setAmount(detail.getQuantity().multiply(detail.getUnitPrice()));
        return detail;
    }

    // endregion
}
