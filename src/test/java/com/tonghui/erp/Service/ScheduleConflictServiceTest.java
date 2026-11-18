package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanParticipant;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import com.tonghui.erp.Data.mapper.RoomInfoMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanBatchMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanParticipantMapper;
import com.tonghui.erp.Service.impl.ScheduleConflictService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 排产冲突校验单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleConflictServiceTest {

    @Mock
    private WorkOrderProcessPlanMapper planMapper;
    @Mock
    private WorkOrderProcessPlanBatchMapper batchMapper;
    @Mock
    private WorkOrderProcessPlanParticipantMapper participantMapper;
    @Mock
    private EquipmentMapper equipmentMapper;
    @Mock
    private RoomInfoMapper roomInfoMapper;

    @InjectMocks
    private ScheduleConflictService conflictService;

    /**
     * 后道工序开始早于紧前工序开始：应报工序顺序冲突
     */
    @Test
    void shouldReportSequenceConflictWhenNextStartsEarlier() {
        when(planMapper.selectOverlappingSavedPlans(any(), any(), anyLong())).thenReturn(List.of());
        when(batchMapper.selectByPlanIds(any())).thenReturn(List.of());

        WorkOrderProcessPlan first = plan(1, 1, "2026-11-20T08:00:00", "2026-11-20T09:00:00");
        WorkOrderProcessPlan second = plan(2, 2, "2026-11-20T07:30:00", "2026-11-20T08:30:00");

        var conflicts = conflictService.check(1L, new ArrayList<>(List.of(first, second)), List.of());

        assertTrue(conflicts.stream().anyMatch(c -> "工序顺序".equals(c.getType())));
    }

    /**
     * 后道工序开始晚于紧前开始，允许时段重叠
     */
    @Test
    void shouldAllowOverlappingTimeWhenNextStartsLater() {
        when(planMapper.selectOverlappingSavedPlans(any(), any(), anyLong())).thenReturn(List.of());
        when(batchMapper.selectByPlanIds(any())).thenReturn(List.of());

        WorkOrderProcessPlan first = plan(1, 1, "2026-11-20T08:00:00", "2026-11-20T09:00:00");
        WorkOrderProcessPlan second = plan(2, 2, "2026-11-20T08:30:00", "2026-11-20T09:30:00");

        var conflicts = conflictService.check(1L, new ArrayList<>(List.of(first, second)), List.of());

        assertEquals(0, conflicts.size());
    }

    /**
     * 同一设备时段重叠：应报设备占用
     */
    @Test
    void shouldReportEquipmentConflict() {
        when(planMapper.selectOverlappingSavedPlans(any(), any(), anyLong())).thenReturn(List.of());
        when(batchMapper.selectByPlanIds(any())).thenReturn(List.of());

        WorkOrderProcessPlan first = plan(1, 1, "2026-11-20T08:00:00", "2026-11-20T09:00:00");
        WorkOrderProcessPlan second = plan(2, 2, "2026-11-20T08:30:00", "2026-11-20T09:30:00");
        first.setEquipmentId(203);
        second.setEquipmentId(203);
        first.setEquipmentName("热风循环烘箱");
        second.setEquipmentName("热风循环烘箱");

        var conflicts = conflictService.check(1L, new ArrayList<>(List.of(first, second)), List.of());

        assertTrue(conflicts.stream().anyMatch(c -> "设备占用".equals(c.getType())));
    }

    /**
     * 同一人在同一时段被安排到两道工序：应报人员冲突
     */
    @Test
    void shouldReportPersonConflictWhenSamePersonOverlaps() {
        when(planMapper.selectOverlappingSavedPlans(any(), any(), anyLong())).thenReturn(List.of());
        when(batchMapper.selectByPlanIds(any())).thenReturn(List.of());

        WorkOrderProcessPlan first = plan(1, 1, "2026-11-20T08:00:00", "2026-11-20T10:00:00");
        WorkOrderProcessPlan second = plan(2, 2, "2026-11-20T09:00:00", "2026-11-20T11:00:00");
        List<WorkOrderProcessPlanParticipant> participants = List.of(
                participant(1L, 71L), participant(2L, 71L));

        var conflicts = conflictService.check(1L, new ArrayList<>(List.of(first, second)),
                List.of(), participants);

        assertTrue(conflicts.stream().anyMatch(c -> "人员冲突".equals(c.getType())));
    }

    /**
     * 未排产工序（计划起止为空）：不参与冲突校验
     */
    @Test
    void shouldIgnoreUnscheduledProcess() {
        when(planMapper.selectOverlappingSavedPlans(any(), any(), anyLong())).thenReturn(List.of());
        when(batchMapper.selectByPlanIds(any())).thenReturn(List.of());

        WorkOrderProcessPlan unscheduled = plan(1, 1, "2026-11-20T08:00:00", "2026-11-20T09:00:00");
        unscheduled.setPlanStartTime(null);
        unscheduled.setPlanEndTime(null);

        var conflicts = conflictService.check(1L, new ArrayList<>(List.of(unscheduled)), List.of());

        assertEquals(0, conflicts.size());
    }

    private WorkOrderProcessPlanParticipant participant(Long planId, Long personnelId) {
        WorkOrderProcessPlanParticipant participant = new WorkOrderProcessPlanParticipant();
        participant.setPlanId(planId);
        participant.setPersonnelId(personnelId);
        return participant;
    }

    private WorkOrderProcessPlan plan(long id, int step, String start, String end) {
        WorkOrderProcessPlan plan = new WorkOrderProcessPlan();
        plan.setId(id);
        plan.setWorkOrderId(1L);
        plan.setWorkOrderCode("GD-TEST");
        plan.setProcessTypeId(100L + step);
        plan.setProcessTypeName("工序" + step);
        plan.setStepOrder(step);
        plan.setPlanStartTime(LocalDateTime.parse(start));
        plan.setPlanEndTime(LocalDateTime.parse(end));
        return plan;
    }
}
