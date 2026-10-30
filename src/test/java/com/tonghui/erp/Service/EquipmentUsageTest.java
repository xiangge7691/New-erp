package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Equipment.EquipmentUsageRecordDto;
import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.EquipmentMaintenanceMapper;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 设备使用记录与冲突校验测试
 * <p>
 * 覆盖设备状态拦截、使用时间重叠拦截、同批内冲突、维保日期冲突、
 * 首尾相接放行、非法时间拦截、按设备导出等核心逻辑，
 * 全部用例使用事务回滚，不污染测试库。
 * </p>
 */
@SpringBootTest
@Transactional
public class EquipmentUsageTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /**
     * 工单工序执行记录服务
     */
    @Autowired
    private WorkOrderProcessExecutionService workOrderProcessExecutionService;

    /**
     * 设备使用记录服务
     */
    @Autowired
    private EquipmentUsageService equipmentUsageService;

    /**
     * 设备维保记录服务
     */
    @Autowired
    private EquipmentMaintenanceService equipmentMaintenanceService;

    /**
     * 设备Mapper（测试数据准备）
     */
    @Autowired
    private EquipmentMapper equipmentMapper;

    /**
     * 设备维保记录Mapper（测试数据准备）
     */
    @Autowired
    private EquipmentMaintenanceMapper equipmentMaintenanceMapper;

    // endregion

    // region 测试常量
    // ===================================
    // 测试常量
    // ===================================

    /**
     * 测试用工单ID序列（使用较大的正数，避免与测试库现有工单冲突）
     */
    private static final AtomicLong WORK_ORDER_ID_SEQUENCE = new AtomicLong(9_000_000_000L);

    // endregion

    // region 使用时间冲突
    // ===================================
    // 使用时间冲突
    // ===================================

    /**
     * 不同工单使用同一设备且时间重叠 → 拦截
     */
    @Test
    public void testOverlappingUsageRejected() {
        Equipment equipment = createEquipment("正常");
        long firstWorkOrderId = nextWorkOrderId();
        long secondWorkOrderId = nextWorkOrderId();

        workOrderProcessExecutionService.batchSave(firstWorkOrderId,
                List.of(buildExecution(firstWorkOrderId, equipment.getEquipmentId(),
                        LocalDateTime.of(2026, 10, 15, 8, 0),
                        LocalDateTime.of(2026, 10, 15, 12, 0))));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> workOrderProcessExecutionService.batchSave(secondWorkOrderId,
                        List.of(buildExecution(secondWorkOrderId, equipment.getEquipmentId(),
                                LocalDateTime.of(2026, 10, 15, 11, 0),
                                LocalDateTime.of(2026, 10, 15, 13, 0)))));
        assertTrue(exception.getMessage().contains("使用时间冲突"), "应提示设备使用时间冲突");
    }

    /**
     * 首尾相接（上一工序 12:00 结束、下一工序 12:00 开始）→ 允许
     */
    @Test
    public void testAdjacentUsageAllowed() {
        Equipment equipment = createEquipment("正常");
        long firstWorkOrderId = nextWorkOrderId();
        long secondWorkOrderId = nextWorkOrderId();

        workOrderProcessExecutionService.batchSave(firstWorkOrderId,
                List.of(buildExecution(firstWorkOrderId, equipment.getEquipmentId(),
                        LocalDateTime.of(2026, 10, 15, 8, 0),
                        LocalDateTime.of(2026, 10, 15, 12, 0))));

        // 不抛异常即通过
        workOrderProcessExecutionService.batchSave(secondWorkOrderId,
                List.of(buildExecution(secondWorkOrderId, equipment.getEquipmentId(),
                        LocalDateTime.of(2026, 10, 15, 12, 0),
                        LocalDateTime.of(2026, 10, 15, 14, 0))));
    }

    /**
     * 同一批提交内两道工序使用同一设备时间重叠 → 拦截
     */
    @Test
    public void testInBatchOverlapRejected() {
        Equipment equipment = createEquipment("正常");
        long workOrderId = nextWorkOrderId();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> workOrderProcessExecutionService.batchSave(workOrderId, List.of(
                        buildExecution(workOrderId, equipment.getEquipmentId(),
                                LocalDateTime.of(2026, 10, 15, 8, 0),
                                LocalDateTime.of(2026, 10, 15, 12, 0)),
                        buildExecution(workOrderId, equipment.getEquipmentId(),
                                LocalDateTime.of(2026, 10, 15, 10, 0),
                                LocalDateTime.of(2026, 10, 15, 11, 0)))));
        assertTrue(exception.getMessage().contains("时间重叠"), "应提示同工单内工序时间重叠");
    }

    /**
     * 结束时间早于开始时间 → 拦截
     */
    @Test
    public void testInvalidTimeRangeRejected() {
        Equipment equipment = createEquipment("正常");
        long workOrderId = nextWorkOrderId();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> workOrderProcessExecutionService.batchSave(workOrderId,
                        List.of(buildExecution(workOrderId, equipment.getEquipmentId(),
                                LocalDateTime.of(2026, 10, 15, 12, 0),
                                LocalDateTime.of(2026, 10, 15, 8, 0)))));
        assertTrue(exception.getMessage().contains("结束时间必须晚于开始时间"), "应提示结束时间必须晚于开始时间");
    }

    // endregion

    // region 设备状态与维保冲突
    // ===================================
    // 设备状态与维保冲突
    // ===================================

    /**
     * 停用设备安排生产使用 → 拦截
     */
    @Test
    public void testDisabledEquipmentRejected() {
        Equipment equipment = createEquipment("停用");
        long workOrderId = nextWorkOrderId();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> workOrderProcessExecutionService.batchSave(workOrderId,
                        List.of(buildExecution(workOrderId, equipment.getEquipmentId(),
                                LocalDateTime.of(2026, 10, 15, 8, 0),
                                LocalDateTime.of(2026, 10, 15, 12, 0)))));
        assertTrue(exception.getMessage().contains("不可安排生产使用"), "应提示设备状态不可用");
    }

    /**
     * 使用日期与设备维保日期同一天（跨日使用命中次日维保）→ 拦截
     */
    @Test
    public void testMaintenanceDateConflictRejected() {
        Equipment equipment = createEquipment("正常");
        long workOrderId = nextWorkOrderId();

        EquipmentMaintenance maintenance = new EquipmentMaintenance();
        maintenance.setEquipmentId(equipment.getEquipmentId().longValue());
        maintenance.setMaintenanceType("定期检修");
        maintenance.setMaintenanceStartTime(LocalDateTime.of(2026, 10, 16, 0, 0));
        maintenance.setMaintenanceEndTime(LocalDateTime.of(2026, 10, 16, 23, 59, 59));
        maintenance.setMaintenanceContent("测试维保");
        equipmentMaintenanceMapper.insert(maintenance);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> workOrderProcessExecutionService.batchSave(workOrderId,
                        List.of(buildExecution(workOrderId, equipment.getEquipmentId(),
                                LocalDateTime.of(2026, 10, 15, 23, 0),
                                LocalDateTime.of(2026, 10, 16, 1, 0)))));
        assertTrue(exception.getMessage().contains("维保"), "应提示设备维保时间冲突");
    }

    /**
     * 登记维保时与既有设备使用时间重叠 → 拦截
     */
    @Test
    public void testMaintenanceOverlapWithUsageRejected() {
        Equipment equipment = createEquipment("正常");
        long workOrderId = nextWorkOrderId();
        workOrderProcessExecutionService.batchSave(workOrderId,
                List.of(buildExecution(workOrderId, equipment.getEquipmentId(),
                        LocalDateTime.of(2026, 10, 15, 8, 0),
                        LocalDateTime.of(2026, 10, 15, 12, 0))));

        EquipmentMaintenance maintenance = new EquipmentMaintenance();
        maintenance.setEquipmentId(equipment.getEquipmentId().longValue());
        maintenance.setMaintenanceType("故障维修");
        maintenance.setMaintenanceStartTime(LocalDateTime.of(2026, 10, 15, 10, 0));
        maintenance.setMaintenanceEndTime(LocalDateTime.of(2026, 10, 15, 11, 0));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> equipmentMaintenanceService.saveWithAutoCalc(maintenance));
        assertTrue(exception.getMessage().contains("占用"), "应提示维保时间与设备使用时间冲突");
    }

    /**
     * 维保时间与使用时间首尾相接（使用 12:00 结束、维保 12:00 开始）→ 允许
     */
    @Test
    public void testAdjacentMaintenanceAllowed() {
        Equipment equipment = createEquipment("正常");
        long workOrderId = nextWorkOrderId();
        workOrderProcessExecutionService.batchSave(workOrderId,
                List.of(buildExecution(workOrderId, equipment.getEquipmentId(),
                        LocalDateTime.of(2026, 10, 15, 8, 0),
                        LocalDateTime.of(2026, 10, 15, 12, 0))));

        EquipmentMaintenance maintenance = new EquipmentMaintenance();
        maintenance.setEquipmentId(equipment.getEquipmentId().longValue());
        maintenance.setMaintenanceType("日常保养");
        maintenance.setMaintenanceStartTime(LocalDateTime.of(2026, 10, 15, 12, 0));
        maintenance.setMaintenanceEndTime(LocalDateTime.of(2026, 10, 15, 14, 0));

        // 不抛异常即通过
        equipmentMaintenanceService.saveWithAutoCalc(maintenance);
    }

    // endregion

    // region 放行场景与导出
    // ===================================
    // 放行场景与导出
    // ===================================

    /**
     * 仅选设备未填时间（工艺模板初始化场景）→ 允许，且不校验设备状态
     */
    @Test
    public void testEquipmentWithoutTimeAllowed() {
        Equipment equipment = createEquipment("停用");
        long workOrderId = nextWorkOrderId();

        // 不抛异常即通过
        workOrderProcessExecutionService.batchSave(workOrderId,
                List.of(buildExecution(workOrderId, equipment.getEquipmentId(), null, null)));
    }

    /**
     * 按设备导出：生成非空 xlsx，且分页查询能查到使用记录
     */
    @Test
    public void testExportAndSearchByEquipment() throws Exception {
        Equipment equipment = createEquipment("正常");
        long workOrderId = nextWorkOrderId();
        workOrderProcessExecutionService.batchSave(workOrderId,
                List.of(buildExecution(workOrderId, equipment.getEquipmentId(),
                        LocalDateTime.of(2026, 10, 15, 8, 0),
                        LocalDateTime.of(2026, 10, 15, 12, 0))));

        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(0);
        pageRequest.setPageSize(10);
        PagedResult<EquipmentUsageRecordDto> page = equipmentUsageService.search(
                equipment.getEquipmentId(), null, null, null, "2026-10-15", "2026-10-15", pageRequest);
        assertTrue(page.getTotalCount() >= 1, "按设备应能查询到使用记录");
        assertTrue(page.getItems().stream()
                        .anyMatch(item -> equipment.getEquipmentId().equals(item.getEquipmentId())
                                && Long.valueOf(240L).equals(item.getDurationMinutes())),
                "使用时长应为 240 分钟");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        equipmentUsageService.exportToExcel(equipment.getEquipmentId(), null, null, null,
                "2026-10-15", "2026-10-15", buffer);
        assertTrue(buffer.size() > 0, "导出文件不应为空");
    }

    // endregion

    // region 测试数据辅助方法
    // ===================================
    // 测试数据辅助方法
    // ===================================

    /**
     * 创建测试设备
     *
     * @param status 设备状态
     * @return 已保存的设备
     */
    private Equipment createEquipment(String status) {
        String unique = String.valueOf(System.nanoTime());
        Equipment equipment = new Equipment();
        equipment.setEquipmentName("测试设备-" + unique);
        equipment.setFixedAssetCode("TEST-" + unique);
        equipment.setEquipmentType("生产设备");
        equipment.setEquipmentStatus(status);
        equipmentMapper.insert(equipment);
        return equipment;
    }

    /**
     * 构建工单工序执行记录
     *
     * @param workOrderId 工单ID
     * @param equipmentId 设备ID
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @return 工序执行记录
     */
    private WorkOrderProcessExecution buildExecution(Long workOrderId, Integer equipmentId,
                                                     LocalDateTime startTime, LocalDateTime endTime) {
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setWorkOrderId(workOrderId);
        execution.setEquipmentId(equipmentId);
        execution.setStepOrder(1);
        execution.setProcessTypeName("测试工序");
        execution.setStartTime(startTime);
        execution.setEndTime(endTime);
        execution.setStatus("待执行");
        return execution;
    }

    /**
     * 生成测试用工单ID
     *
     * @return 工单ID
     */
    private long nextWorkOrderId() {
        return WORK_ORDER_ID_SEQUENCE.incrementAndGet();
    }

    // endregion
}
