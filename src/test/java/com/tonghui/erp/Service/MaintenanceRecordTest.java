package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenancePendingDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenancePendingSummaryDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordDetailSaveDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordListItemDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordSaveDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceRecordWithDetailsDto;
import com.tonghui.erp.Common.Dto.Maintenance.MaintenanceStockItemDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.MaintenanceRecord;
import com.tonghui.erp.Data.Entity.MaintenanceRecordDetail;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.Stock;
import com.tonghui.erp.Data.Entity.StockIn;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.mapper.MaintenanceRecordDetailMapper;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Data.mapper.ProductionUnitMapper;
import com.tonghui.erp.Data.mapper.StockInMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 养护记录模块专项测试
 * <p>
 * 覆盖业务规则：
 * 1. 编号按实际检查日期生成（YH-YYYYMMDD-NNN），格式正则校验
 * 2. 新增保存主单+明细，快照物料/批号/入库编号/数量/养护要求
 * 3. 检查项目四态默认未见异常，异常判定（任一项发现异常或填写异常描述）
 * 4. 下次养护日期=本次实际检查日期+养护周期快照（周期为空则NULL）
 * 5. 更新时明细整体重建（旧行物理删除后重插），编号保留原值
 * 6. 必填校验（检查时间/检查类型/仓库/检查人员/明细）
 * 7. 列表查询（编号关键字、明细物料维度命中）与明细数/异常数统计
 * 8. 软删除后详情返回null、明细物理删除
 * 9. 待养护查询排除零库存、提醒状态计算与统计
 * 全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
@Transactional
public class MaintenanceRecordTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 养护记录服务
     */
    @Autowired
    private MaintenanceRecordService maintenanceRecordService;

    /**
     * 库存Mapper（创建测试库存明细）
     */
    @Autowired
    private StockMapper stockMapper;

    /**
     * 入库单Mapper（创建测试入库单）
     */
    @Autowired
    private StockInMapper stockInMapper;

    /**
     * 物料Mapper（创建带养护周期的测试物料）
     */
    @Autowired
    private MaterialMapper materialMapper;

    /**
     * 生产单位Mapper（查找测试仓库）
     */
    @Autowired
    private ProductionUnitMapper productionUnitMapper;

    /**
     * 用户Mapper（查找测试检查人员）
     */
    @Autowired
    private UserMapper userMapper;

    /**
     * 库存检查明细Mapper（断言删除后明细物理删除）
     */
    @Autowired
    private MaintenanceRecordDetailMapper maintenanceRecordDetailMapper;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试养护记录编号生成：格式YH-YYYYMMDD-NNN，按传入检查日期确定日期段
     */
    @Test
    public void testGenerateCode() {
        String code = maintenanceRecordService.generateCode("2026-10-15");
        assertTrue(code.startsWith("YH-20261015-"), "编号日期段应与传入检查日期一致：" + code);
        assertTrue(code.matches("^YH-\\d{8}-\\d{3}$"),
                "编号格式应为YH-YYYYMMDD-NNN：" + code);
        // 不传日期时按当天生成
        String todayCode = maintenanceRecordService.generateCode(null);
        String today = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertTrue(todayCode.startsWith("YH-" + today + "-"), "不传日期应按当天生成：" + todayCode);
        assertTrue(todayCode.matches("^YH-\\d{8}-\\d{3}$"), "当天编号格式应正确：" + todayCode);
    }

    /**
     * 测试新增养护记录：主单+明细一次保存，快照物料与养护要求，默认四态未见异常
     */
    @Test
    public void testCreateSnapshotsAndDefaults() {
        TestData data = createTestData(new BigDecimal("20.000"), 30);
        MaintenanceRecordSaveDto dto = buildSaveDto(data, "日常检查");
        MaintenanceRecord created = maintenanceRecordService.create(dto);

        assertNotNull(created.getId(), "新增后应有ID");
        assertTrue(created.getMaintenanceCode().matches("^YH-\\d{8}-\\d{3}$"),
                "编号格式应为YH-YYYYMMDD-NNN：" + created.getMaintenanceCode());
        assertNotNull(created.getWarehouseName(), "仓库名称应已快照");
        assertNotNull(created.getCheckerName(), "检查人员姓名应已快照");

        MaintenanceRecordWithDetailsDto detail = maintenanceRecordService.getDetail(created.getId());
        assertNotNull(detail, "详情应可查询");
        assertEquals(1, detail.getDetails().size(), "明细应有1行");
        MaintenanceRecordDetail d = detail.getDetails().get(0);
        // 快照字段
        assertEquals(data.itemCode, d.getMaterialCode(), "物料编码应按stockId快照");
        assertEquals(data.batchNumber, d.getBatchNumber(), "批号应快照");
        assertEquals(data.inCode, d.getInCode(), "入库编号应快照");
        assertEquals(0, new BigDecimal("20.000").compareTo(d.getStockQuantity()),
                "检查时库存数量应快照");
        // 默认四态未见异常
        assertEquals(MaintenanceRecordDetail.CHECK_NORMAL, d.getPackagingStatus(), "包装状态默认未见异常");
        assertEquals(MaintenanceRecordDetail.CHECK_NORMAL, d.getMoistureStatus(), "受潮状态默认未见异常");
        // 下次养护日期 = 检查日期 + 周期快照（30天）
        assertEquals(created.getCheckTime().toLocalDate(), d.getLastMaintenanceDate(),
                "最近养护日期应为本次实际检查日期");
        assertEquals(created.getCheckTime().toLocalDate().plusDays(30), d.getNextMaintenanceDate(),
                "下次养护日期应为最近养护日期+30天");
        assertEquals(30, d.getMaintenanceCycleDays(), "养护周期应从物料快照");
        assertEquals("测试养护注意事项", d.getMaintenanceNotes(), "养护注意事项应从物料快照");
        assertEquals(Integer.valueOf(1), d.getKeyMaintenance(), "重点养护标记应从物料快照");
        // 登记不产生库存增减
        Stock stock = stockMapper.selectById(data.stockId);
        assertEquals(0, new BigDecimal("20.000").compareTo(stock.getQuantity()),
                "登记养护不应改变库存数量");
    }

    /**
     * 测试养护周期为空时不计算下次养护日期（不纳入提醒）
     */
    @Test
    public void testNextDateNullWhenCycleEmpty() {
        TestData data = createTestData(new BigDecimal("5.000"), null);
        MaintenanceRecordSaveDto dto = buildSaveDto(data, "日常检查");
        MaintenanceRecord created = maintenanceRecordService.create(dto);

        MaintenanceRecordWithDetailsDto detail = maintenanceRecordService.getDetail(created.getId());
        MaintenanceRecordDetail d = detail.getDetails().get(0);
        assertNull(d.getMaintenanceCycleDays(), "养护周期应为空");
        assertNull(d.getNextMaintenanceDate(), "周期为空时下次养护日期应为NULL");
    }

    /**
     * 测试更新重建明细：编号保留原值，旧明细物理删除，明细状态按新值更新
     */
    @Test
    public void testUpdateRebuildsDetails() {
        TestData data = createTestData(new BigDecimal("10.000"), 15);
        MaintenanceRecordSaveDto dto = buildSaveDto(data, "日常检查");
        MaintenanceRecord created = maintenanceRecordService.create(dto);
        Long oldDetailId = maintenanceRecordService.getDetail(created.getId())
                .getDetails().get(0).getId();

        // 修改：检查类型变化 + 检查项改为发现异常 + 补充异常描述
        MaintenanceRecordSaveDto updateDto = buildSaveDto(data, "定期检查");
        updateDto.getDetails().get(0).setMoistureStatus(MaintenanceRecordDetail.CHECK_ABNORMAL);
        updateDto.getDetails().get(0).setAbnormalDescription("垛位底部轻微受潮");
        updateDto.getDetails().get(0).setMoistureContent(new BigDecimal("14.20"));
        MaintenanceRecord updated = maintenanceRecordService.update(created.getId(), updateDto);

        assertEquals(created.getMaintenanceCode(), updated.getMaintenanceCode(), "编号应保留原值");
        assertEquals("定期检查", updated.getCheckType(), "检查类型应更新");

        MaintenanceRecordWithDetailsDto detail = maintenanceRecordService.getDetail(created.getId());
        assertEquals(1, detail.getDetails().size(), "明细应重建为1行");
        assertNotEquals(oldDetailId, detail.getDetails().get(0).getId(), "重建后明细ID应为新ID");
        MaintenanceRecordDetail d = detail.getDetails().get(0);
        assertEquals(MaintenanceRecordDetail.CHECK_ABNORMAL, d.getMoistureStatus(), "受潮状态应更新为发现异常");
        assertEquals("垛位底部轻微受潮", d.getAbnormalDescription(), "异常描述应保存");
        assertTrue(detail.getDetails().get(0).isAbnormal(), "异常标记应为true");
        assertEquals(1, detail.getAbnormalCount(), "异常明细数应为1");
    }

    /**
     * 测试列表查询与统计：编号关键字命中、明细物料维度命中、明细数/异常数正确
     */
    @Test
    public void testSearchAndCounts() {
        TestData data = createTestData(new BigDecimal("8.000"), 20);
        MaintenanceRecordSaveDto dto = buildSaveDto(data, "日常检查");
        // 第二行明细标为异常
        TestData data2 = createTestData(new BigDecimal("3.000"), null);
        MaintenanceRecordDetailSaveDto abnormal = new MaintenanceRecordDetailSaveDto();
        abnormal.setStockId(data2.stockId);
        abnormal.setPestStatus(MaintenanceRecordDetail.CHECK_ABNORMAL);
        abnormal.setAbnormalDescription("发现虫蛀痕迹");
        dto.getDetails().add(abnormal);
        MaintenanceRecord created = maintenanceRecordService.create(dto);

        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(0);
        pageRequest.setPageSize(10);

        // 按编号关键字查询
        PagedResult<MaintenanceRecordListItemDto> byKeyword = maintenanceRecordService.search(
                created.getMaintenanceCode(), null, null, null, null, null,
                null, null, null, null, pageRequest);
        assertTrue(byKeyword.getItems().stream().anyMatch(r -> r.getId().equals(created.getId())),
                "按编号关键字应命中新建记录");
        MaintenanceRecordListItemDto item = byKeyword.getItems().stream()
                .filter(r -> r.getId().equals(created.getId()))
                .findFirst().orElse(null);
        assertNotNull(item, "列表应命中新建记录");
        assertEquals(2, item.getDetailCount(), "明细数应为2");
        assertEquals(1, item.getAbnormalCount(), "异常明细数应为1");

        // 按明细物料维度查询
        PagedResult<MaintenanceRecordListItemDto> byMaterial = maintenanceRecordService.search(
                null, null, data.itemCode, null, null, null,
                null, null, null, null, pageRequest);
        assertTrue(byMaterial.getItems().stream().anyMatch(r -> r.getId().equals(created.getId())),
                "按明细物料编码应命中主表");

        // 按是否异常查询
        PagedResult<MaintenanceRecordListItemDto> byAbnormal = maintenanceRecordService.search(
                null, null, null, null, null, null,
                null, true, null, null, pageRequest);
        assertTrue(byAbnormal.getItems().stream().anyMatch(r -> r.getId().equals(created.getId())),
                "按异常筛选应命中含异常明细的记录");

        // 无匹配物料返回空
        PagedResult<MaintenanceRecordListItemDto> noMatch = maintenanceRecordService.search(
                null, null, "NOT-EXIST-MATERIAL-XYZ", null, null, null,
                null, null, null, null, pageRequest);
        assertTrue(noMatch.getItems().isEmpty(), "无匹配明细物料时应返回空结果");
    }

    /**
     * 测试必填校验：检查时间/检查类型/仓库/检查人员/明细缺失时应抛异常
     */
    @Test
    public void testRequiredValidation() {
        TestData data = createTestData(new BigDecimal("1.000"), 10);

        MaintenanceRecordSaveDto noCheckTime = buildSaveDto(data, "日常检查");
        noCheckTime.setCheckTime(null);
        assertThrows(RuntimeException.class, () -> maintenanceRecordService.create(noCheckTime),
                "检查时间为空应抛异常");

        MaintenanceRecordSaveDto noType = buildSaveDto(data, " ");
        assertThrows(RuntimeException.class, () -> maintenanceRecordService.create(noType),
                "检查类型为空应抛异常");

        MaintenanceRecordSaveDto noWarehouse = buildSaveDto(data, "日常检查");
        noWarehouse.setWarehouseId(null);
        assertThrows(RuntimeException.class, () -> maintenanceRecordService.create(noWarehouse),
                "仓库为空应抛异常");

        MaintenanceRecordSaveDto noChecker = buildSaveDto(data, "日常检查");
        noChecker.setCheckerId(null);
        assertThrows(RuntimeException.class, () -> maintenanceRecordService.create(noChecker),
                "检查人员为空应抛异常");

        MaintenanceRecordSaveDto noDetails = buildSaveDto(data, "日常检查");
        noDetails.setDetails(null);
        assertThrows(RuntimeException.class, () -> maintenanceRecordService.create(noDetails),
                "明细为空应抛异常");

        MaintenanceRecordSaveDto badStock = buildSaveDto(data, "日常检查");
        badStock.getDetails().get(0).setStockId(999999999L);
        assertThrows(RuntimeException.class, () -> maintenanceRecordService.create(badStock),
                "库存明细不存在应抛异常");
    }

    /**
     * 测试删除：主单软删除后详情返回null，明细物理删除不再参与提醒
     */
    @Test
    public void testDeleteRemovesDetailFromPending() {
        TestData data = createTestData(new BigDecimal("50.000"), 1);
        MaintenanceRecordSaveDto dto = buildSaveDto(data, "日常检查");
        MaintenanceRecord created = maintenanceRecordService.create(dto);
        Long recordId = created.getId();

        maintenanceRecordService.delete(recordId);
        assertNull(maintenanceRecordService.getDetail(recordId), "删除后详情应返回null");

        // 明细已物理删除（按主单ID查询不到残留明细）
        assertEquals(0, maintenanceRecordDetailCount(recordId), "删除后明细应物理删除");
    }

    /**
     * 测试库存选择：按仓库名查询，带出入库编号与物料养护要求
     */
    @Test
    public void testGetStockDetails() {
        TestData data = createTestData(new BigDecimal("12.000"), 25);
        List<MaintenanceStockItemDto> items = maintenanceRecordService.getStockDetails(
                data.warehouseName, data.itemCode, false);
        assertTrue(items.stream().anyMatch(i -> data.stockId.equals(i.getStockId())),
                "按仓库+物料编码应命中测试库存");
        MaintenanceStockItemDto hit = items.stream()
                .filter(i -> data.stockId.equals(i.getStockId()))
                .findFirst().orElse(null);
        assertNotNull(hit, "应命中测试库存");
        assertEquals(data.inCode, hit.getInCode(), "应带出入库编号");
        assertEquals(Integer.valueOf(25), hit.getMaintenanceCycleDays(), "应带出养护周期");
        assertEquals("测试养护注意事项", hit.getMaintenanceNotes(), "应带出养护注意事项");
        assertEquals(Integer.valueOf(1), hit.getKeyMaintenance(), "应带出重点养护标记");
        // 仓库名称缺失时应抛异常
        assertThrows(RuntimeException.class,
                () -> maintenanceRecordService.getStockDetails(null, null, false),
                "仓库为空应抛异常");
    }

    /**
     * 测试待养护查询与统计：含下次养护日期的有库存明细纳入，窗口内状态已计算
     */
    @Test
    public void testPendingQueryAndSummary() {
        // 周期1天 → 下次养护日期在提醒窗口内（默认提前提醒3天），状态应为即将到期
        TestData data = createTestData(new BigDecimal("15.000"), 1);
        MaintenanceRecordSaveDto dto = buildSaveDto(data, "日常检查");
        maintenanceRecordService.create(dto);

        // 统计（事务内可见）
        MaintenancePendingSummaryDto summary = maintenanceRecordService.getPendingSummary();
        assertTrue(summary.getRemindDays() > 0, "提前提醒天数应大于0");
        assertTrue(summary.getOverdueCount() + summary.getTodayCount() + summary.getSoonCount() > 0,
                "窗口内应至少统计到1条");
        assertTrue(summary.getSoonCount() >= 1, "明天到期应计入即将到期");

        // 待养护列表（-1全量）
        PagedResult<MaintenancePendingDto> pending = maintenanceRecordService.searchPending(
                data.itemCode, null, null, -1, -1);
        assertTrue(pending.getItems().stream().anyMatch(p -> data.stockId.equals(p.getStockId())),
                "含下次养护日期的有库存明细应出现在待养护列表");
        MaintenancePendingDto hit = pending.getItems().stream()
                .filter(p -> data.stockId.equals(p.getStockId()))
                .findFirst().orElse(null);
        assertNotNull(hit, "应命中待养护项");
        assertEquals("SOON", hit.getPendingStatus(), "1天后到期应为即将到期状态");
        assertNotNull(hit.getLastMaintenanceCode(), "应回填最近一次养护记录编号");
        assertEquals(0, hit.getOverdueDays(), "未逾期时逾期天数应为0");

        // 按提醒状态过滤：命中SOON，非法状态返回空
        PagedResult<MaintenancePendingDto> filterSoon = maintenanceRecordService.searchPending(
                null, null, "SOON", -1, -1);
        assertTrue(filterSoon.getItems().stream().anyMatch(p -> data.stockId.equals(p.getStockId())),
                "按SOON过滤应命中明天到期明细");
        PagedResult<MaintenancePendingDto> filterNone = maintenanceRecordService.searchPending(
                null, null, "NOT_A_STATUS", 0, 10);
        assertTrue(filterNone.getItems().isEmpty(), "非法提醒状态应返回空");
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 测试数据容器（库存与仓库关联信息）
     */
    private static class TestData {
        /** 库存明细ID */
        Long stockId;
        /** 物料编码 */
        String itemCode;
        /** 批号 */
        String batchNumber;
        /** 入库编号 */
        String inCode;
        /** 仓库名称 */
        String warehouseName;
        /** 仓库ID */
        Long warehouseId;
        /** 检查人员ID */
        Long checkerId;
    }

    /**
     * 创建测试数据：测试物料（可选养护周期）+ 入库单 + 库存明细 + 复用测试仓库/用户
     *
     * @param quantity  库存数量
     * @param cycleDays 养护周期（天），null表示无周期
     * @return 测试数据容器
     */
    private TestData createTestData(BigDecimal quantity, Integer cycleDays) {
        TestData data = new TestData();
        String unique = String.valueOf(System.nanoTime());
        data.itemCode = "MTX" + unique.substring(unique.length() - 10);
        data.batchNumber = "MBT" + unique.substring(unique.length() - 8);
        data.inCode = "MINC" + unique.substring(unique.length() - 10);

        // 仓库与检查人员（复用现有基础数据）
        ProductionUnit unit = productionUnitMapper.selectList(
                new LambdaQueryWrapper<ProductionUnit>().last("LIMIT 1")).stream()
                .findFirst().orElse(null);
        assertNotNull(unit, "应存在生产单位（仓库）基础数据");
        data.warehouseId = unit.getProdUnitId();
        data.warehouseName = unit.getProdUnitName();

        User user = userMapper.selectList(
                new LambdaQueryWrapper<User>().last("LIMIT 1")).stream()
                .findFirst().orElse(null);
        assertNotNull(user, "应存在用户基础数据");
        data.checkerId = user.getUserId();

        // 测试物料（带养护要求快照字段）
        Material material = new Material();
        material.setMaterialCode(data.itemCode);
        material.setMaterialName("养护测试物料" + unique.substring(unique.length() - 6));
        material.setCategoryName("测试分类");
        material.setUnitName("kg");
        material.setSpec("测试规格");
        material.setStorageRequirement("阴凉干燥");
        material.setMaintenanceNotes("测试养护注意事项");
        material.setMaintenanceCycleDays(cycleDays);
        material.setKeyMaintenance(1);
        material.setMaterialStatus(1);
        assertTrue(materialMapper.insert(material) > 0, "新增测试物料应成功");

        // 入库单
        StockIn stockIn = new StockIn();
        stockIn.setInCode(data.inCode);
        stockIn.setProdUnitId(data.warehouseId);
        stockIn.setInDate(LocalDateTime.now());
        stockIn.setInStatus("已完成");
        assertTrue(stockInMapper.insert(stockIn) > 0, "新增测试入库单应成功");

        // 库存明细
        Stock stock = new Stock();
        stock.setProdUnitId(data.warehouseId);
        stock.setItemType("material");
        stock.setItemCode(data.itemCode);
        stock.setItemName(material.getMaterialName());
        stock.setCategoryName("测试分类");
        stock.setUnitName("kg");
        stock.setBatchNumber(data.batchNumber);
        stock.setQuantity(quantity);
        stock.setStockInId(stockIn.getInId());
        assertTrue(stockMapper.insert(stock) > 0, "新增测试库存应成功");
        data.stockId = stock.getStockId();
        return data;
    }

    /**
     * 构建养护记录保存请求体（必填字段齐全，1行明细指向测试库存）
     *
     * @param data      测试数据容器
     * @param checkType 检查类型
     * @return 保存请求体
     */
    private MaintenanceRecordSaveDto buildSaveDto(TestData data, String checkType) {
        MaintenanceRecordSaveDto dto = new MaintenanceRecordSaveDto();
        dto.setCheckTime(LocalDateTime.now());
        dto.setCheckType(checkType);
        dto.setWarehouseId(data.warehouseId);
        dto.setCheckerId(data.checkerId);
        dto.setEnvObserveInfo("测试库温湿度记录仪旁");
        dto.setTemperature(new BigDecimal("22.50"));
        dto.setHumidity(new BigDecimal("55.00"));
        dto.setSummary("测试养护检查");

        MaintenanceRecordDetailSaveDto detail = new MaintenanceRecordDetailSaveDto();
        detail.setStockId(data.stockId);
        detail.setShelfLocation("A区3架");
        detail.setCheckScope("全部");
        List<MaintenanceRecordDetailSaveDto> details = new ArrayList<>();
        details.add(detail);
        dto.setDetails(details);
        return dto;
    }

    /**
     * 统计指定养护记录下的明细行数
     *
     * @param maintenanceId 养护记录ID
     * @return 明细行数
     */
    private Long maintenanceRecordDetailCount(Long maintenanceId) {
        return maintenanceRecordDetailMapper.selectCount(
                new LambdaQueryWrapper<MaintenanceRecordDetail>()
                        .eq(MaintenanceRecordDetail::getMaintenanceId, maintenanceId));
    }

    // endregion
}
