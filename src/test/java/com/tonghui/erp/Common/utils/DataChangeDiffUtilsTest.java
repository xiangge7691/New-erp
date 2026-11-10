package com.tonghui.erp.Common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonghui.erp.Common.Config.LogProperties;
import com.tonghui.erp.Common.Dto.Log.FieldChange;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据变更差异工具单元测试
 * <p>
 * 覆盖字段差异、审计字段排除、敏感字段脱敏、软删除识别、新增/删除快照与长值截断
 * </p>
 */
public class DataChangeDiffUtilsTest {

    // region 测试对象
    // ===================================
    // 测试对象
    // ===================================

    private final DataChangeDiffUtils diffUtils =
            new DataChangeDiffUtils(new ObjectMapper(), new JdbcTemplate(), new LogProperties());

    // endregion

    // region 差异用例
    // ===================================
    // 差异用例
    // ===================================

    /**
     * 正常更新只记录业务字段变化，排除更新时间等技术字段
     */
    @Test
    public void testUpdateDiff() {
        Map<String, Object> before = row();
        before.put("status", "待采购");
        before.put("order_time", null);
        before.put("updated_time", LocalDateTime.of(2026, 11, 10, 9, 0));

        Map<String, Object> after = row();
        after.put("status", "运输中");
        after.put("order_time", LocalDateTime.of(2026, 11, 10, 9, 30));
        after.put("updated_time", LocalDateTime.of(2026, 11, 10, 9, 30));

        DataChangeDiffUtils.Result result = diffUtils.diff("purchase_orders", before, after);

        assertNotNull(result);
        assertEquals(DataChangeDiffUtils.TYPE_UPDATE, result.getChangeType());
        assertEquals(2, result.getFieldCount());
        List<String> fields = result.getChangedFields().stream().map(FieldChange::getField).toList();
        assertTrue(fields.contains("status"));
        assertTrue(fields.contains("order_time"));
        assertFalse(fields.contains("updated_time"));
        assertNotNull(result.getBeforeJson());
        assertNotNull(result.getAfterJson());
    }

    /**
     * 只有审计字段变化时返回 null，不记录无意义的变更
     */
    @Test
    public void testNoBusinessChange() {
        Map<String, Object> before = row();
        before.put("updated_time", LocalDateTime.of(2026, 11, 10, 9, 0));
        Map<String, Object> after = row();
        after.put("updated_time", LocalDateTime.of(2026, 11, 10, 10, 0));

        assertNull(diffUtils.diff("purchase_orders", before, after));
    }

    /**
     * 密码等敏感字段在字段差异和整行快照中都被脱敏
     */
    @Test
    public void testSensitiveFieldMasked() {
        Map<String, Object> before = row();
        before.put("password", "old-password");
        Map<String, Object> after = row();
        after.put("password", "new-password");

        DataChangeDiffUtils.Result result = diffUtils.diff("user", before, after);

        assertNotNull(result);
        FieldChange change = result.getChangedFields().get(0);
        assertEquals("******", change.getBefore());
        assertEquals("******", change.getAfter());
        assertFalse(result.getBeforeJson().contains("old-password"));
        assertFalse(result.getAfterJson().contains("new-password"));
    }

    /**
     * is_deleted 0→1 识别为 DELETE
     */
    @Test
    public void testSoftDeleteDetected() {
        Map<String, Object> before = row();
        before.put("is_deleted", 0);
        Map<String, Object> after = row();
        after.put("is_deleted", 1);

        DataChangeDiffUtils.Result result = diffUtils.diff("system_config", before, after);

        assertNotNull(result);
        assertEquals(DataChangeDiffUtils.TYPE_DELETE, result.getChangeType());
    }

    /**
     * 新增记录记录非空业务字段
     */
    @Test
    public void testInsertSnapshot() {
        Map<String, Object> after = row();
        after.put("config_value", "v1");
        after.put("created_time", LocalDateTime.now());

        DataChangeDiffUtils.Result result = diffUtils.insert("system_config", after);

        assertNotNull(result);
        assertEquals(DataChangeDiffUtils.TYPE_INSERT, result.getChangeType());
        assertTrue(result.getChangedFields().stream().anyMatch(c -> "config_value".equals(c.getField())));
        assertFalse(result.getChangedFields().stream().anyMatch(c -> "created_time".equals(c.getField())));
    }

    /**
     * 删除记录保留修改前字段值
     */
    @Test
    public void testDeleteSnapshot() {
        Map<String, Object> before = row();
        before.put("config_value", "v1");

        DataChangeDiffUtils.Result result = diffUtils.delete("system_config", before);

        assertNotNull(result);
        assertEquals(DataChangeDiffUtils.TYPE_DELETE, result.getChangeType());
        FieldChange change = result.getChangedFields().stream()
                .filter(c -> "config_value".equals(c.getField()))
                .findFirst().orElseThrow();
        assertEquals("v1", change.getBefore());
        assertNull(change.getAfter());
    }

    /**
     * 超长字段值按配置截断
     */
    @Test
    public void testLongValueTruncated() {
        LogProperties properties = new LogProperties();
        properties.getChange().setMaxValueLength(10);
        DataChangeDiffUtils utils =
                new DataChangeDiffUtils(new ObjectMapper(), new JdbcTemplate(), properties);

        Map<String, Object> before = row();
        before.put("remark", "old");
        Map<String, Object> after = row();
        after.put("remark", "123456789012345");

        DataChangeDiffUtils.Result result = utils.diff("system_config", before, after);

        assertNotNull(result);
        Object value = result.getChangedFields().get(0).getAfter();
        assertTrue(String.valueOf(value).contains("已截断"));
    }

    // endregion

    // region 测试辅助
    // ===================================
    // 测试辅助
    // ===================================

    /**
     * 构造基础行
     *
     * @return 行数据
     */
    private Map<String, Object> row() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1L);
        row.put("updated_by", 1L);
        row.put("version", 0);
        return row;
    }

    // endregion
}
