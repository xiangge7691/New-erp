package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch;
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
import java.util.Set;

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

    /**
     * 拆分批次：除本次批次外，还要和同工单其他工序做设备冲突校验
     */
    @Test
    void shouldCheckSplitBatchAgainstSameWorkOrderSibling() {
        when(planMapper.selectOverlappingSavedPlans(any(), any(), anyLong())).thenReturn(List.of());
        WorkOrderProcessPlan sibling = plan(1, 1, "2026-11-20T08:30:00", "2026-11-20T09:30:00");
        sibling.setStatus("已保存");
        sibling.setEquipmentId(203);
        when(planMapper.selectByWorkOrderIdWithDetails(1L)).thenReturn(List.of(sibling));
        when(batchMapper.selectByPlanIds(any())).thenReturn(List.of());

        WorkOrderProcessPlan splitPlan = plan(2, 2, "2026-11-20T08:00:00", "2026-11-20T09:00:00");
        splitPlan.setStatus("已保存");
        splitPlan.setEquipmentId(203);
        WorkOrderProcessPlanBatch batch = new WorkOrderProcessPlanBatch();
        batch.setId(21L);
        batch.setPlanId(2L);
        batch.setBatchNo(1);
        batch.setPlanStartTime(LocalDateTime.parse("2026-11-20T08:00:00"));
        batch.setPlanEndTime(LocalDateTime.parse("2026-11-20T09:00:00"));
        batch.setEquipmentId(203);

        var conflicts = conflictService.check(1L, new ArrayList<>(List.of(splitPlan)),
                List.of(batch), List.of(), Set.of());

        assertTrue(conflicts.stream().anyMatch(c -> "设备占用".equals(c.getType())), conflicts.toString());
    }

    /**
     * 紧前工序未排时间：不能静默跳过先后校验
     */
    @Test
    void shouldReportConflictWhenPreviousStepHasNoTime() {
        when(planMapper.selectOverlappingSavedPlans(any(), any(), anyLong())).thenReturn(List.of());
        when(batchMapper.selectByPlanIds(any())).thenReturn(List.of());

        WorkOrderProcessPlan first = plan(1, 1, "2026-11-20T08:00:00", "2026-11-20T09:00:00");
        first.setPlanStartTime(null);
        first.setPlanEndTime(null);
        WorkOrderProcessPlan second = plan(2, 2, "2026-11-20T08:30:00", "2026-11-20T09:30:00");

        var conflicts = conflictService.check(1L, new ArrayList<>(List.of(first, second)), List.of());

        assertTrue(conflicts.stream().anyMatch(c -> "工序顺序".equals(c.getType())
                && c.getMessage().contains("未排产时间")), conflicts.toString());
    }

    /**
     * 排产计划早于配置日期：应报配置时间窗冲突
     */
    @Test
    void shouldReportConfigWindowConflict() {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(1L);
        order.setConfigDate(LocalDateTime.parse("2026-11-19T15:18:00"));

        WorkOrderProcessPlan plan = plan(1, 1, "2026-11-10T00:00:00", "2026-11-11T00:00:00");

        var conflicts = conflictService.checkConfigWindow(order, List.of(plan), List.of());

        assertTrue(conflicts.stream().anyMatch(c -> "配置时间窗".equals(c.getType())
                && c.getMessage().contains("早于配置日期")), conflicts.toString());
    }

    /**
     * 拆分工序的先后冲突要落到具体批次，而不是整道工序
     */
    @Test
    void shouldAttributeSequenceConflictToSpecificBatch() {
        when(planMapper.selectOverlappingSavedPlans(any(), any(), anyLong())).thenReturn(List.of());
        when(batchMapper.selectByPlanIds(any())).thenReturn(List.of());
        when(planMapper.selectByWorkOrderIdWithDetails(anyLong())).thenReturn(List.of());

        WorkOrderProcessPlan prev = plan(1, 1, "2026-11-28T23:00:00", "2026-11-28T23:10:00");
        WorkOrderProcessPlan split = plan(2, 2, "2026-11-28T22:00:00", "2026-11-29T00:10:00");
        WorkOrderProcessPlanBatch batchOne = batch(21L, 2L, 1,
                "2026-11-28T22:00:00", "2026-11-29T00:00:00");
        WorkOrderProcessPlanBatch batchTwo = batch(22L, 2L, 2,
                "2026-11-29T00:01:00", "2026-11-29T00:10:00");

        var conflicts = conflictService.check(1L, new ArrayList<>(List.of(prev, split)),
                List.of(batchOne, batchTwo));

        var sequence = conflicts.stream().filter(c -> "工序顺序".equals(c.getType())).toList();
        assertEquals(1, sequence.size(), conflicts.toString());
        assertEquals(21L, sequence.get(0).getBatchId());
        assertTrue(sequence.get(0).getMessage().contains("批次 1"), sequence.get(0).getMessage());
    }

    private WorkOrderProcessPlanParticipant participant(Long planId, Long personnelId) {
        WorkOrderProcessPlanParticipant participant = new WorkOrderProcessPlanParticipant();
        participant.setPlanId(planId);
        participant.setPersonnelId(personnelId);
        return participant;
    }

    private WorkOrderProcessPlanBatch batch(long id, long planId, int no, String start, String end) {
        WorkOrderProcessPlanBatch batch = new WorkOrderProcessPlanBatch();
        batch.setId(id);
        batch.setPlanId(planId);
        batch.setBatchNo(no);
        batch.setPlanStartTime(LocalDateTime.parse(start));
        batch.setPlanEndTime(LocalDateTime.parse(end));
        return batch;
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
