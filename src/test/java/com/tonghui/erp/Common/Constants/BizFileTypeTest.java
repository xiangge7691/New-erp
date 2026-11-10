package com.tonghui.erp.Common.Constants;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业务附件类型注册表测试
 * <p>
 * 纯常量校验，不依赖 Spring 容器和数据库。
 * </p>
 */
class BizFileTypeTest {

    // region 注册表完整性
    // ===================================
    // 注册表完整性
    // ===================================

    /**
     * 每个业务类型必须有编码、表名、主键列、存储目录和至少一个槽位
     */
    @Test
    void everyTypeShouldBeComplete() {
        Set<String> codes = new HashSet<>();
        for (BizFileType type : BizFileType.values()) {
            assertNotNull(type.getCode());
            assertTrue(!type.getCode().isBlank(), "业务类型编码不能为空");
            assertTrue(codes.add(type.getCode()), "业务类型编码重复: " + type.getCode());
            assertTrue(!type.getTableName().isBlank(), "业务表名不能为空: " + type.getCode());
            assertTrue(!type.getPkColumn().isBlank(), "主键列名不能为空: " + type.getCode());
            assertTrue(!type.getStorageDir().isBlank(), "存储目录不能为空: " + type.getCode());
            assertTrue(!type.getKindMaxCount().isEmpty(), "槽位配置不能为空: " + type.getCode());
        }
    }

    /**
     * 业务类型编码必须能反查
     */
    @Test
    void resolveShouldReturnRegisteredType() {
        assertEquals(BizFileType.WORK_ORDER_PROCESS_EXECUTION,
                BizFileType.resolve("WORK_ORDER_PROCESS_EXECUTION"));
        assertEquals(BizFileType.APPROVAL_INSTANCE,
                BizFileType.resolve("APPROVAL_INSTANCE"));
        assertEquals(BizFileType.INSPECTION_RECORD,
                BizFileType.resolve("INSPECTION_RECORD"));
        assertEquals(BizFileType.STOCK_OUT_PRODUCTION,
                BizFileType.resolve("STOCK_OUT_PRODUCTION"));
        assertEquals(BizFileType.STOCK_IN_PRODUCT,
                BizFileType.resolve("STOCK_IN_PRODUCT"));
        assertEquals(BizFileType.STOCK_OUT_SALES,
                BizFileType.resolve("STOCK_OUT_SALES"));
        assertEquals(BizFileType.RETURN_ORDER,
                BizFileType.resolve("RETURN_ORDER"));
    }

    /**
     * 未注册的编码返回 null，由服务层统一报错
     */
    @Test
    void resolveUnknownShouldReturnNull() {
        assertNull(BizFileType.resolve("NOT_EXISTS"));
        assertNull(BizFileType.resolve(null));
    }

    /**
     * 补齐的制剂资料 / 验收 / 工单 / 请检槽位必须已注册
     */
    @Test
    void supplementedKindsShouldBeRegistered() {
        assertEquals(BizFileKinds.DOSSIER_579,
                BizFileType.PREPARATION.normalizeKind(BizFileKinds.DOSSIER_579));
        assertEquals(BizFileKinds.REGISTRATION_CERTIFICATE,
                BizFileType.PREPARATION.normalizeKind(BizFileKinds.REGISTRATION_CERTIFICATE));
        assertEquals(BizFileKinds.PACKAGING_DESIGN,
                BizFileType.PREPARATION.normalizeKind(BizFileKinds.PACKAGING_DESIGN));
        assertEquals(BizFileKinds.VERIFICATION_REPORT,
                BizFileType.PREPARATION.normalizeKind(BizFileKinds.VERIFICATION_REPORT));
        assertEquals(BizFileKinds.INBOUND_ORDER,
                BizFileType.ACCEPTANCE_ORDER.normalizeKind(BizFileKinds.INBOUND_ORDER));
        assertEquals(BizFileKinds.OUTBOUND_ORDER,
                BizFileType.WORK_ORDER.normalizeKind(BizFileKinds.OUTBOUND_ORDER));
        assertEquals(BizFileKinds.INTERMEDIATE_INSPECTION_REQUEST,
                BizFileType.WORK_ORDER.normalizeKind(BizFileKinds.INTERMEDIATE_INSPECTION_REQUEST));
        assertEquals(BizFileKinds.FINISHED_PRODUCT_REPORT,
                BizFileType.INSPECTION_REQUEST.normalizeKind(BizFileKinds.FINISHED_PRODUCT_REPORT));
    }

    // endregion

    // region 槽位校验
    // ===================================
    // 槽位校验
    // ===================================

    /**
     * 空槽位：单槽位类型使用唯一槽位，多槽位类型使用 DEFAULT
     */
    @Test
    void blankKindShouldResolveToDefaultOrSingleKind() {
        assertEquals(BizFileKinds.PROCESS_ATTACHMENT,
                BizFileType.WORK_ORDER_PROCESS_EXECUTION.normalizeKind(null));
        assertEquals(BizFileKinds.PROCESS_ATTACHMENT,
                BizFileType.WORK_ORDER_PROCESS_EXECUTION.normalizeKind(""));
        assertEquals(BizFileKinds.DEFAULT,
                BizFileType.DESTRUCTION_RECORD.normalizeKind(null));
    }

    /**
     * 槽位不区分大小写
     */
    @Test
    void kindShouldBeCaseInsensitive() {
        assertEquals(BizFileKinds.PROCESS_ATTACHMENT,
                BizFileType.WORK_ORDER_PROCESS_EXECUTION.normalizeKind("process_attachment"));
    }

    /**
     * 未声明的槽位应被拒绝
     */
    @Test
    void unsupportedKindShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> BizFileType.WORK_ORDER_PROCESS_EXECUTION.normalizeKind("WAYBILL"));
    }

    /**
     * 槽位数量上限应生效
     */
    @Test
    void maxCountShouldMatchConfiguration() {
        assertEquals(1, BizFileType.WORK_ORDER_PROCESS_EXECUTION.maxCount(BizFileKinds.PROCESS_ATTACHMENT));
        assertEquals(10, BizFileType.WORK_ORDER.maxCount(BizFileKinds.BATCH_RECORD));
        assertEquals(10, BizFileType.ACCEPTANCE_ORDER.maxCount(BizFileKinds.WAYBILL));
        assertEquals(10, BizFileType.INSPECTION_RECORD.maxCount(BizFileKinds.DEFAULT));
    }

    // endregion
}
