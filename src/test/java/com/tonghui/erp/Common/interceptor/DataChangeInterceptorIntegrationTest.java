package com.tonghui.erp.Common.interceptor;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.tonghui.erp.Common.Dto.Log.DataChangeRecord;
import com.tonghui.erp.Common.Dto.Log.DataChangeTraceContext;
import com.tonghui.erp.Common.Dto.Log.FieldChange;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.SystemConfig;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.SystemConfigMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据变更拦截器集成测试
 * <p>
 * 使用 system_config 测试 updateById、wrapper 更新、级联多次更新与逻辑删除的
 * 修改前/后采集，测试事务结束后回滚，不残留业务数据
 * </p>
 */
@SpringBootTest
@Transactional
public class DataChangeInterceptorIntegrationTest {

    // region 依赖
    // ===================================
    // 依赖
    // ===================================

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * updateById 能抓取字段级修改前后值
     */
    @Test
    public void testUpdateByIdCapturesDiff() {
        SystemConfig config = createConfig("v1");

        DataChangeTraceContext.Trace trace = runWithTrace(() -> {
            config.setConfigValue("v2");
            systemConfigMapper.updateById(config);
        });

        assertNotNull(trace);
        assertEquals(1, trace.getChanges().size());
        DataChangeRecord change = trace.getChanges().get(0);
        assertEquals("system_config", change.getTableName());
        assertEquals(String.valueOf(config.getConfigId()), change.getBusinessId());
        assertEquals("UPDATE", change.getChangeType());

        FieldChange field = change.getChangedFields().stream()
                .filter(item -> "config_value".equals(item.getField()))
                .findFirst().orElseThrow();
        assertEquals("v1", field.getBefore());
        assertEquals("v2", field.getAfter());
        assertNotNull(field.getLabel());
        assertFalse(change.getChangedFields().stream()
                .anyMatch(item -> "updated_time".equals(item.getField())));
    }

    /**
     * wrapper 更新能先定位受影响行，再抓取修改前后值
     */
    @Test
    public void testWrapperUpdateCapturesDiff() {
        SystemConfig config = createConfig("v1");

        DataChangeTraceContext.Trace trace = runWithTrace(() ->
                systemConfigMapper.update(null, new UpdateWrapper<SystemConfig>()
                        .eq("config_key", config.getConfigKey())
                        .set("config_value", "v3")));

        assertNotNull(trace);
        assertEquals(1, trace.getChanges().size());
        DataChangeRecord change = trace.getChanges().get(0);
        assertEquals(String.valueOf(config.getConfigId()), change.getBusinessId());
        FieldChange field = change.getChangedFields().stream()
                .filter(item -> "config_value".equals(item.getField()))
                .findFirst().orElseThrow();
        assertEquals("v1", field.getBefore());
        assertEquals("v3", field.getAfter());
    }

    /**
     * 同一 Trace 内多次更新会分别记录，覆盖级联修改场景
     */
    @Test
    public void testCascadeUpdatesCaptured() {
        SystemConfig first = createConfig("v1");
        SystemConfig second = createConfig("v1");

        DataChangeTraceContext.Trace trace = runWithTrace(() -> {
            first.setConfigValue("v2");
            systemConfigMapper.updateById(first);
            second.setConfigValue("v2");
            systemConfigMapper.updateById(second);
        });

        assertNotNull(trace);
        assertEquals(2, trace.getChanges().size());
        assertTrue(trace.getChanges().stream()
                .allMatch(change -> "system_config".equals(change.getTableName())));
        assertTrue(trace.getChanges().stream()
                .map(DataChangeRecord::getBusinessId)
                .toList()
                .containsAll(List.of(String.valueOf(first.getConfigId()), String.valueOf(second.getConfigId()))));
    }

    /**
     * 新增记录能抓取新增后的整行快照
     */
    @Test
    public void testInsertCaptured() {
        SystemConfig config = newConfig("v1");

        DataChangeTraceContext.Trace trace;
        DataChangeTraceContext.begin();
        try {
            systemConfigMapper.insert(config);
        } finally {
            trace = DataChangeTraceContext.end();
        }

        assertNotNull(trace);
        assertEquals(1, trace.getChanges().size());
        DataChangeRecord change = trace.getChanges().get(0);
        assertEquals("INSERT", change.getChangeType());
        assertEquals(String.valueOf(config.getConfigId()), change.getBusinessId());
        assertNotNull(change.getAfterJson());
        assertTrue(change.getChangedFields().stream()
                .anyMatch(item -> "config_value".equals(item.getField())));
    }

    /**
     * wrapper 删除能先定位受影响行，再按行记录删除前数据
     */
    @Test
    public void testWrapperDeleteCaptured() {
        SystemConfig config = createConfig("v1");

        DataChangeTraceContext.Trace trace = runWithTrace(() ->
                systemConfigMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SystemConfig>()
                        .eq("config_key", config.getConfigKey())));

        assertNotNull(trace);
        assertEquals(1, trace.getChanges().size());
        DataChangeRecord change = trace.getChanges().get(0);
        assertEquals("DELETE", change.getChangeType());
        assertEquals(String.valueOf(config.getConfigId()), change.getBusinessId());
        assertNotNull(change.getBeforeJson());
    }

    /**
     * 逻辑删除识别为 DELETE，并保留删除前数据
     */
    @Test
    public void testSoftDeleteCaptured() {
        SystemConfig config = createConfig("v1");

        DataChangeTraceContext.Trace trace = runWithTrace(() ->
                systemConfigMapper.deleteById(config.getConfigId()));

        assertNotNull(trace);
        assertEquals(1, trace.getChanges().size());
        DataChangeRecord change = trace.getChanges().get(0);
        assertEquals("DELETE", change.getChangeType());
        assertEquals(String.valueOf(config.getConfigId()), change.getBusinessId());
        assertNotNull(change.getBeforeJson());
    }

    /**
     * 自定义 SQL（@DataChangeAudit）能按注解定位受影响行并记录删除
     */
    @Test
    public void testCustomSqlAuditCaptured() {
        ProductionUnit unit = new ProductionUnit();
        unit.setProdUnitCode("__custom_audit_" + System.nanoTime());
        unit.setProdUnitName("自定义SQL测试单位");
        unit.setProdUnitManager("测试");
        unit.setProdUnitStatus(1);
        unit.setIsDeleted(0);
        unit.setVersion(0);
        productionUnitMapper.insert(unit);
        Long prodUnitId = unit.getProdUnitId();

        Stock stock = new Stock();
        stock.setProdUnitId(prodUnitId);
        stock.setItemType("material");
        stock.setItemCode("TEST-" + prodUnitId);
        stock.setItemName("测试物料");
        stock.setCategoryName("原料");
        stock.setUnitName("kg");
        stock.setBatchNumber("B" + prodUnitId);
        stock.setIsDeleted(0);
        stock.setVersion(0);
        stockMapper.insert(stock);

        DataChangeTraceContext.Trace trace = runWithTrace(() ->
                stockMapper.softDeleteByProdUnitId(prodUnitId));

        assertNotNull(trace);
        assertEquals(1, trace.getChanges().size());
        DataChangeRecord change = trace.getChanges().get(0);
        assertEquals("stock", change.getTableName());
        assertEquals("DELETE", change.getChangeType());
        assertEquals(String.valueOf(stock.getStockId()), change.getBusinessId());
    }

    // endregion

    // region 测试辅助
    // ===================================
    // 测试辅助
    // ===================================

    /**
     * 创建测试配置项
     *
     * @param value 配置值
     * @return 配置项
     */
    private SystemConfig createConfig(String value) {
        SystemConfig config = newConfig(value);
        systemConfigMapper.insert(config);
        return config;
    }

    /**
     * 构造测试配置项（不落库）
     *
     * @param value 配置值
     * @return 配置项
     */
    private SystemConfig newConfig(String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("__change_test_" + System.nanoTime());
        config.setConfigValue(value);
        config.setConfigName("变更日志测试");
        config.setConfigGroup("测试");
        config.setValueType("string");
        config.setStatus(1);
        config.setIsDeleted(0);
        config.setVersion(0);
        return config;
    }

    /**
     * 在变更 Trace 中执行操作
     *
     * @param action 操作
     * @return 结束的 Trace
     */
    private DataChangeTraceContext.Trace runWithTrace(Runnable action) {
        DataChangeTraceContext.begin();
        try {
            action.run();
            return DataChangeTraceContext.end();
        } catch (RuntimeException e) {
            DataChangeTraceContext.end();
            throw e;
        }
    }

    // endregion
}
