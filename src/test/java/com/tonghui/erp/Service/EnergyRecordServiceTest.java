package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Energy.EnergyRecordPageResult;
import com.tonghui.erp.Data.Entity.EnergyRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 能耗记录服务测试
 * <p>
 * 覆盖计量单位映射、实用量/总价自动计算、参数校验、汇总统计、软删除过滤
 * （业务数据在事务中回滚，不污染数据库）
 * </p>
 */
@SpringBootTest
@Transactional
public class EnergyRecordServiceTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 能耗记录服务
     */
    @Autowired
    private EnergyRecordService energyRecordService;

    // endregion

    // region 工具方法
    // ===================================
    // 工具方法
    // ===================================

    /**
     * 构造基础能耗记录（电，2026-08，表底 4010/4510，单价 4.84）
     */
    private EnergyRecord buildElectricRecord() {
        EnergyRecord record = new EnergyRecord();
        record.setMonth("2026-08");
        record.setEnergyType("电");
        record.setLastMeterReading(new BigDecimal("4010.000"));
        record.setCurrentMeterReading(new BigDecimal("4510.000"));
        record.setUnitPrice(new BigDecimal("4.840"));
        return record;
    }

    // endregion

    // region 新增与自动计算
    // ===================================
    // 新增与自动计算
    // ===================================

    /**
     * 测试：新增时计量单位自动映射（电→度）
     */
    @Test
    public void testUnitMappingElectric() {
        EnergyRecord record = energyRecordService.create(buildElectricRecord(), null);
        assertEquals("度", record.getUnit(), "电的计量单位应为度");
    }

    /**
     * 测试：新增时计量单位自动映射（自来水/燃气→立方米）
     */
    @Test
    public void testUnitMappingWaterAndGas() {
        EnergyRecord water = new EnergyRecord();
        water.setMonth("2026-08");
        water.setEnergyType("自来水");
        water.setActualUsage(new BigDecimal("30.000"));
        water.setUnitPrice(new BigDecimal("26.000"));
        EnergyRecord savedWater = energyRecordService.create(water, null);
        assertEquals("立方米", savedWater.getUnit(), "自来水的计量单位应为立方米");

        EnergyRecord gas = new EnergyRecord();
        gas.setMonth("2026-08");
        gas.setEnergyType("燃气");
        gas.setActualUsage(new BigDecimal("40.000"));
        gas.setUnitPrice(new BigDecimal("29.500"));
        EnergyRecord savedGas = energyRecordService.create(gas, null);
        assertEquals("立方米", savedGas.getUnit(), "燃气的计量单位应为立方米");
    }

    /**
     * 测试：实用量与总价自动计算（表底差值 + 用量×单价）
     */
    @Test
    public void testAutoCalculateUsageAndAmount() {
        EnergyRecord record = energyRecordService.create(buildElectricRecord(), null);
        assertEquals(0, new BigDecimal("500.000").compareTo(record.getActualUsage()),
                "实用量应自动计算为 本月表底-上月表底 = 500");
        assertEquals(0, new BigDecimal("2420.00").compareTo(record.getTotalAmount()),
                "总价应自动计算为 500×4.84 = 2420.00");
        assertEquals("超级管理员", record.getOperatorName(), "操作人应取当前登录用户姓名");
        assertNotNull(record.getOperatorId(), "操作人ID不应为空");
    }

    /**
     * 测试：手动提供的实用量与总价不被覆盖
     */
    @Test
    public void testManualUsageAndAmountKept() {
        EnergyRecord record = buildElectricRecord();
        record.setActualUsage(new BigDecimal("520.000"));
        record.setTotalAmount(new BigDecimal("2500.00"));
        EnergyRecord saved = energyRecordService.create(record, null);
        assertEquals(0, new BigDecimal("520.000").compareTo(saved.getActualUsage()), "手动实用量应保留");
        assertEquals(0, new BigDecimal("2500.00").compareTo(saved.getTotalAmount()), "手动总价应保留");
    }

    /**
     * 测试：编辑只改单价时总价联动重算
     */
    @Test
    public void testUpdatePriceRecalculatesAmount() {
        EnergyRecord record = energyRecordService.create(buildElectricRecord(), null);
        EnergyRecord update = new EnergyRecord();
        update.setUnitPrice(new BigDecimal("4.900"));
        EnergyRecord updated = energyRecordService.update(record.getRecordId(), update, null);
        assertEquals(0, new BigDecimal("2450.00").compareTo(updated.getTotalAmount()),
                "改单价后总价应联动为 500×4.90 = 2450.00");
        assertEquals("超级管理员", updated.getOperatorName(), "编辑不应覆盖操作人");
    }

    // endregion

    // region 参数校验
    // ===================================
    // 参数校验
    // ===================================

    /**
     * 测试：实用量为负数时拒绝保存
     */
    @Test
    public void testNegativeUsageRejected() {
        EnergyRecord record = buildElectricRecord();
        record.setActualUsage(new BigDecimal("-1.000"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> energyRecordService.create(record, null),
                "实用量为负数时应抛出异常");
        assertTrue(ex.getMessage().contains("负数"), "错误消息应提示负数");
    }

    /**
     * 测试：非法能耗类型拒绝保存
     */
    @Test
    public void testInvalidEnergyTypeRejected() {
        EnergyRecord record = buildElectricRecord();
        record.setEnergyType("蒸汽");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> energyRecordService.create(record, null),
                "非法能耗类型应抛出异常");
        assertTrue(ex.getMessage().contains("不合法"), "错误消息应提示类型不合法");
    }

    /**
     * 测试：非法月份格式拒绝保存
     */
    @Test
    public void testInvalidMonthRejected() {
        EnergyRecord record = buildElectricRecord();
        record.setMonth("202608");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> energyRecordService.create(record, null),
                "非法月份格式应抛出异常");
        assertTrue(ex.getMessage().contains("月份格式"), "错误消息应提示月份格式");
    }

    // endregion

    // region 查询与汇总
    // ===================================
    // 查询与汇总
    // ===================================

    /**
     * 测试：分页查询返回汇总（总金额 + 各类型金额）
     * <p>
     * 用 energyType=电 精确筛选，避免受数据库中其他测试数据影响
     * </p>
     */
    @Test
    public void testPageQueryWithSummary() {
        EnergyRecord electric = energyRecordService.create(buildElectricRecord(), null);
        EnergyRecord water = new EnergyRecord();
        water.setMonth("2026-08");
        water.setEnergyType("自来水");
        water.setActualUsage(new BigDecimal("30.000"));
        water.setUnitPrice(new BigDecimal("26.000"));
        energyRecordService.create(water, null);

        EnergyRecordPageResult result = energyRecordService.pageQuery("2026-08", "电", 0, 20);
        assertTrue(result.getItems().stream().anyMatch(r -> r.getRecordId().equals(electric.getRecordId())),
                "列表中应包含本次新建的电费记录");
        assertNotNull(result.getSummary(), "应返回汇总");
        assertEquals(0, result.getSummary().getByType().get("电").compareTo(new BigDecimal("2420.00")),
                "汇总中电费应为2420.00");
        assertEquals(0, result.getSummary().getByType().get("自来水").compareTo(BigDecimal.ZERO),
                "按电筛选时自来水费汇总应为0（汇总与筛选条件联动）");
        assertEquals(0, result.getSummary().getTotalAmount().compareTo(new BigDecimal("2420.00")),
                "按电筛选时汇总总金额应为2420.00");
    }

    /**
     * 测试：软删除后列表不再包含该记录
     */
    @Test
    public void testSoftDeleteFiltersFromList() {
        EnergyRecord record = energyRecordService.create(buildElectricRecord(), null);
        energyRecordService.delete(record.getRecordId());
        EnergyRecordPageResult result = energyRecordService.pageQuery("2026-08", "电", 0, 20);
        assertTrue(result.getItems().stream().noneMatch(r -> r.getRecordId().equals(record.getRecordId())),
                "软删除后的记录不应出现在列表中");
    }

    /**
     * 测试：删除不存在的记录抛出异常
     */
    @Test
    public void testDeleteNotExistThrows() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> energyRecordService.delete(999999L),
                "删除不存在的记录应抛出异常");
        assertTrue(ex.getMessage().contains("不存在"), "错误消息应提示记录不存在");
    }

    // endregion
}
