package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Common.Dto.Equipment.EquipmentUsageConflictDto;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.EquipmentMaintenanceMapper;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 设备使用冲突校验测试
 * <p>
 * 覆盖「进行中（未填结束时间）」记录的冲突降级：只返回提醒、不阻断保存；
 * 双方结束时间都完整的区间冲突仍然阻断。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EquipmentUsageConflictValidatorTest {

    @Mock
    private EquipmentMapper equipmentMapper;
    @Mock
    private WorkOrderProcessExecutionMapper executionMapper;
    @Mock
    private EquipmentMaintenanceMapper maintenanceMapper;

    @InjectMocks
    private EquipmentUsageConflictValidator validator;

    /**
     * 进行中的记录与其他工单占用重叠：返回提醒而不是抛异常
     */
    @Test
    void shouldReturnWarningWhenOpenRowOverlaps() {
        when(equipmentMapper.selectByIdForUpdate(203)).thenReturn(equipment(203));
        when(executionMapper.selectConflictRecords(any(), any(), any(), any()))
                .thenReturn(List.of(conflict("2026-11-24T16:37:00", "2026-11-24T16:38:00")));
        when(maintenanceMapper.selectMaintenanceConflicts(any(), any(), any())).thenReturn(List.of());

        WorkOrderProcessExecution row = execution("2026-11-19T16:00:00", null);

        List<String> warnings = validator.validate(1L, List.of(row), Set.of());

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("使用时间冲突"), warnings.get(0));
    }

    /**
     * 双方结束时间都完整且重叠：仍然阻断保存
     */
    @Test
    void shouldThrowWhenCompletedRowsOverlap() {
        when(equipmentMapper.selectByIdForUpdate(203)).thenReturn(equipment(203));
        when(executionMapper.selectConflictRecords(any(), any(), any(), any()))
                .thenReturn(List.of(conflict("2026-11-19T17:00:00", "2026-11-19T17:30:00")));
        when(maintenanceMapper.selectMaintenanceConflicts(any(), any(), any())).thenReturn(List.of());

        WorkOrderProcessExecution row = execution("2026-11-19T16:00:00", "2026-11-19T18:00:00");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(1L, List.of(row), Set.of()));
        assertTrue(ex.getMessage().contains("使用时间冲突"), ex.getMessage());
    }

    private Equipment equipment(Integer id) {
        Equipment equipment = new Equipment();
        equipment.setEquipmentId(id);
        equipment.setEquipmentName("槽形混合机");
        equipment.setEquipmentStatus("正常");
        equipment.setIsDeleted(0);
        return equipment;
    }

    private EquipmentUsageConflictDto conflict(String start, String end) {
        EquipmentUsageConflictDto dto = new EquipmentUsageConflictDto();
        dto.setRecordId(900L);
        dto.setEquipmentId(203);
        dto.setEquipmentName("槽形混合机");
        dto.setWorkOrderId(2L);
        dto.setWorkOrderCode("GD202611200001");
        dto.setProcessName("混合");
        dto.setBatchNumber("未填写");
        dto.setStartTime(LocalDateTime.parse(start));
        dto.setEndTime(LocalDateTime.parse(end));
        return dto;
    }

    private WorkOrderProcessExecution execution(String start, String end) {
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setId(1L);
        execution.setWorkOrderId(1L);
        execution.setStepOrder(5);
        execution.setProcessTypeName("混合");
        execution.setEquipmentId(203);
        execution.setStartTime(LocalDateTime.parse(start));
        execution.setEndTime(end == null ? null : LocalDateTime.parse(end));
        return execution;
    }
}
