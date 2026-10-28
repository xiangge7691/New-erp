package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.SupplierAudit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 供应商审计记录新增字段测试
 * <p>
 * 验证 supplier_audit.social_credit_code、supplier_audit.reviewer
 * 两个新增字段能够正常写入与读取，全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
@Transactional
public class RoomAndSupplierAuditFieldTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 供应商审核记录服务
     */
    @Autowired
    private SupplierAuditService supplierAuditService;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试供应商审计记录统一社会信用代码与复核人字段的写入与读取
     */
    @Test
    public void testSupplierAuditCreditCodeAndReviewerPersist() {
        SupplierAudit audit = new SupplierAudit();
        audit.setSupplierId(1L);
        audit.setSupplierName("测试供应商-审计字段");
        audit.setSocialCreditCode("91430100MA4L000001");
        audit.setReviewer("测试复核人");
        audit.setAuditDate(new java.util.Date());
        audit.setAuditResult("合格");
        supplierAuditService.addSupplierAudit(audit);

        SupplierAudit loaded = supplierAuditService.getSupplierAuditById(audit.getId());
        assertNotNull(loaded, "审计记录应能按ID查询到");
        assertEquals("91430100MA4L000001", loaded.getSocialCreditCode(), "统一社会信用代码应正确持久化");
        assertEquals("测试复核人", loaded.getReviewer(), "复核人应正确持久化");
    }

    // endregion
}
