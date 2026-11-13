package com.tonghui.erp.Common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物料来源策略测试
 * <p>
 * 验证产品汇总“物料来源”口径：采购与院内领料二选一（采购优先），
 * 两者都无但有物料入库时为直接入库；生产领料单不参与来源判定。
 * </p>
 */
public class MaterialSourcePolicyTest {

    /**
     * 采购与院内领料同时存在时，采购优先
     */
    @Test
    public void testPurchaseWinsWhenBothEvidenceExist() {
        assertEquals(MaterialSourcePolicy.PURCHASE, MaterialSourcePolicy.resolve(true, true, true),
                "同时有采购与院内领料证据时应显示采购");
        assertEquals(MaterialSourcePolicy.PURCHASE, MaterialSourcePolicy.resolve(true, false, false));
    }

    /**
     * 无采购、仅院内领料 → 院内领料
     */
    @Test
    public void testRequisitionWhenNoPurchase() {
        assertEquals(MaterialSourcePolicy.REQUISITION, MaterialSourcePolicy.resolve(false, true, true));
        assertEquals(MaterialSourcePolicy.REQUISITION, MaterialSourcePolicy.resolve(false, true, false));
    }

    /**
     * 无采购、无院内领料 → 有物料入库为直接入库，否则为空
     */
    @Test
    public void testDirectOrNullWhenNoPurchaseOrRequisition() {
        assertEquals(MaterialSourcePolicy.DIRECT, MaterialSourcePolicy.resolve(false, false, true));
        assertNull(MaterialSourcePolicy.resolve(false, false, false), "无任何证据时不应返回来源");
    }

    /**
     * 验收单来源类型判定
     */
    @Test
    public void testSourceTypeMatching() {
        assertTrue(MaterialSourcePolicy.isPurchaseSource("采购入库"), "采购入库应为采购来源");
        assertTrue(MaterialSourcePolicy.isPurchaseSource("退货重发"), "退货重发应为采购来源");
        assertFalse(MaterialSourcePolicy.isPurchaseSource("领料入库"), "领料入库不应为采购来源");
        assertFalse(MaterialSourcePolicy.isPurchaseSource("直接入库"), "直接入库不应为采购来源");
        assertTrue(MaterialSourcePolicy.isRequisitionSource("领料入库"), "领料入库应为院内领料来源");
        assertFalse(MaterialSourcePolicy.isRequisitionSource("采购入库"), "采购入库不应为院内领料来源");
        assertFalse(MaterialSourcePolicy.isRequisitionSource(null), "空来源不应为院内领料来源");
    }

    /**
     * 编码转显示文案
     */
    @Test
    public void testLabel() {
        assertEquals("采购", MaterialSourcePolicy.label(MaterialSourcePolicy.PURCHASE));
        assertEquals("院内领料", MaterialSourcePolicy.label(MaterialSourcePolicy.REQUISITION));
        assertEquals("直接入库", MaterialSourcePolicy.label(MaterialSourcePolicy.DIRECT));
        assertNull(MaterialSourcePolicy.label(null));
    }
}
