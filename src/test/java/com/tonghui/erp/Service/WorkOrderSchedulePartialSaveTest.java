package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Scheduling.PlanBatchDto;
import com.tonghui.erp.Common.Dto.Scheduling.ProcessPlanSaveDto;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleConflictDto;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleSaveRequest;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Data.Entity.Unit;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import com.tonghui.erp.Data.mapper.PreparationProcessTemplateMapper;
import com.tonghui.erp.Data.mapper.ProcessTypeMapper;
import com.tonghui.erp.Data.mapper.ProductionPlanMapper;
import com.tonghui.erp.Data.mapper.ProductionTeamMapper;
import com.tonghui.erp.Data.mapper.ProductionTeamMemberMapper;
import com.tonghui.erp.Data.mapper.RoomInfoMapper;
import com.tonghui.erp.Data.mapper.UnitMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanBatchMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanParticipantMapper;
import com.tonghui.erp.Service.impl.ScheduleConflictService;
import com.tonghui.erp.Service.impl.WorkOrderScheduleServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 排产保存单元测试
 * <p>
 * 覆盖按单道工序增量保存的场景：本次未提交的其他工序计划不能被删除。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderSchedulePartialSaveTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private PreparationProcessTemplateMapper templateMapper;
    @Mock
    private ProcessTypeMapper processTypeMapper;
    @Mock
    private ProductionTeamMapper teamMapper;
    @Mock
    private ProductionPlanMapper productionPlanMapper;
    @Mock
    private ProductionTeamMemberMapper teamMemberMapper;
    @Mock
    private WorkOrderProcessPlanMapper planMapper;
    @Mock
    private WorkOrderProcessPlanBatchMapper batchMapper;
    @Mock
    private WorkOrderProcessPlanParticipantMapper participantMapper;
    @Mock
    private WorkOrderProcessExecutionMapper executionMapper;
    @Mock
    private RoomInfoMapper roomInfoMapper;
    @Mock
    private EquipmentMapper equipmentMapper;
    @Mock
    private UnitMapper unitMapper;
    @Mock
    private ScheduleConflictService conflictService;

    @InjectMocks
    private WorkOrderScheduleServiceImpl scheduleService;

    // endregion

    // region 测试准备
    // ===================================
    // 测试准备
    // ===================================

    /**
     * 初始化实体表信息缓存：LambdaUpdateWrapper 需要 MyBatis-Plus 的列缓存
     */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, WorkOrderProcessPlan.class);
    }

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 只提交第 3 道工序时：第 3 道工序更新，第 1 道工序的计划保留不删除
     */
    @Test
    void shouldNotDeleteOtherProcessPlansWhenSavingSingleProcess() {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(470L);
        order.setWorkOrderCode("GD202611110001");
        order.setPreparationId(244L);
        order.setBatchQty(new BigDecimal("7001"));
        when(workOrderMapper.selectById(470L)).thenReturn(order);

        WorkOrderProcessPlan first = new WorkOrderProcessPlan();
        first.setId(25L);
        first.setWorkOrderId(470L);
        first.setProcessTypeId(189L);
        first.setStepOrder(1);

        WorkOrderProcessPlan sterilization = new WorkOrderProcessPlan();
        sterilization.setId(27L);
        sterilization.setWorkOrderId(470L);
        sterilization.setProcessTypeId(88L);
        sterilization.setStepOrder(3);
        when(planMapper.selectList(any())).thenReturn(List.of(first, sterilization));
        when(planMapper.selectByWorkOrderIdWithDetails(470L)).thenReturn(List.of());

        ProcessType processType = new ProcessType();
        processType.setProcessName("灭菌");
        when(processTypeMapper.selectById(88L)).thenReturn(processType);

        RoomInfo room = new RoomInfo();
        room.setRoomName("灭菌间");
        when(roomInfoMapper.selectById(108)).thenReturn(room);

        Equipment equipment = new Equipment();
        equipment.setEquipmentName("卧式压力蒸汽灭菌器");
        when(equipmentMapper.selectById(219)).thenReturn(equipment);

        when(conflictService.check(any(), any(), any())).thenReturn(List.of());
        when(conflictService.check(any(), any(), any(), any())).thenReturn(List.of());

        ProcessPlanSaveDto dto = new ProcessPlanSaveDto();
        dto.setPlanId(27L);
        dto.setProcessTypeId(88L);
        dto.setStepOrder(3);
        dto.setPlanQty(new BigDecimal("7001"));
        dto.setRoomId(108);
        dto.setEquipmentId(219);
        ScheduleSaveRequest request = new ScheduleSaveRequest();
        request.setWorkOrderId(470L);
        request.setProcesses(List.of(dto));

        scheduleService.save(470L, request, false);

        // 第 3 道工序按工序顺序合并到既有计划
        verify(planMapper).updateById(argThat(p -> Long.valueOf(27L).equals(p.getId())));
        // 未提交的第 1 道工序不能被删除
        verify(planMapper, never()).deleteById(anyLong());
    }

    /**
     * 详情返回的「已排产」标识完全取保存值，不按计划起止时间推算
     */
    @Test
    void shouldReturnStoredScheduledFlagWithoutCalculation() {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(470L);
        order.setWorkOrderCode("GD202611110001");
        when(workOrderMapper.selectById(470L)).thenReturn(order);

        // 有计划时间但前端标记未排产
        WorkOrderProcessPlan unscheduled = new WorkOrderProcessPlan();
        unscheduled.setId(25L);
        unscheduled.setWorkOrderId(470L);
        unscheduled.setProcessTypeId(189L);
        unscheduled.setProcessTypeName("干燥饮片");
        unscheduled.setStepOrder(1);
        unscheduled.setPlanStartTime(java.time.LocalDateTime.of(2026, 11, 18, 0, 0));
        unscheduled.setPlanEndTime(java.time.LocalDateTime.of(2026, 11, 18, 0, 2));
        unscheduled.setScheduled(0);
        unscheduled.setUnitId(2L);

        // 无计划时间但前端标记已排产
        WorkOrderProcessPlan scheduled = new WorkOrderProcessPlan();
        scheduled.setId(26L);
        scheduled.setWorkOrderId(470L);
        scheduled.setProcessTypeId(63L);
        scheduled.setProcessTypeName("粉碎");
        scheduled.setStepOrder(2);
        scheduled.setScheduled(1);
        scheduled.setUnitId(2L);

        Unit unit = new Unit();
        unit.setUnitId(2L);
        unit.setUnitName("千克");
        unit.setSymbol("kg");
        when(unitMapper.selectBatchIds(any())).thenReturn(List.of(unit));

        when(planMapper.selectByWorkOrderIdWithDetails(470L)).thenReturn(List.of(unscheduled, scheduled));
        when(batchMapper.selectByPlanIds(any())).thenReturn(List.of());
        when(participantMapper.selectByPlanIdsWithPersonnel(any())).thenReturn(List.of());
        when(executionMapper.selectByWorkOrderIdWithDetails(470L)).thenReturn(List.of());
        when(conflictService.check(any(), any(), any())).thenReturn(List.of());

        var detail = scheduleService.getDetail(470L);

        org.junit.jupiter.api.Assertions.assertFalse(detail.getProcesses().get(0).getScheduled());
        org.junit.jupiter.api.Assertions.assertTrue(detail.getProcesses().get(1).getScheduled());
        org.junit.jupiter.api.Assertions.assertEquals(2L, detail.getProcesses().get(0).getUnitId());
        org.junit.jupiter.api.Assertions.assertEquals("kg", detail.getProcesses().get(0).getUnitSymbol());
    }

    /**
     * 拆分批次数量合计不强制等于工序计划数量，允许按实际安排拆分
     */
    @Test
    void shouldAllowSplitQuantityNotEqualToPlanQuantity() {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(470L);
        order.setWorkOrderCode("GD202611110001");
        when(workOrderMapper.selectById(470L)).thenReturn(order);

        WorkOrderProcessPlan plan = new WorkOrderProcessPlan();
        plan.setId(27L);
        plan.setWorkOrderId(470L);
        plan.setProcessTypeId(88L);
        plan.setStepOrder(3);
        plan.setPlanQty(new BigDecimal("7001"));
        plan.setUnitId(2L);
        when(planMapper.selectById(27L)).thenReturn(plan);

        ProcessType processType = new ProcessType();
        processType.setProcessName("炼蜜");
        when(processTypeMapper.selectById(88L)).thenReturn(processType);

        when(conflictService.check(any(), any(), any())).thenReturn(List.of());
        when(planMapper.selectByWorkOrderIdWithDetails(470L)).thenReturn(List.of());

        PlanBatchDto first = new PlanBatchDto();
        first.setBatchNo(1);
        first.setBatchName("批次 1");
        first.setPlanStartTime(LocalDateTime.of(2026, 11, 20, 1, 0));
        first.setPlanEndTime(LocalDateTime.of(2026, 11, 20, 1, 18));
        first.setPlanQty(new BigDecimal("3500"));

        PlanBatchDto second = new PlanBatchDto();
        second.setBatchNo(2);
        second.setBatchName("批次 2");
        second.setPlanStartTime(LocalDateTime.of(2026, 11, 20, 2, 18));
        second.setPlanEndTime(LocalDateTime.of(2026, 11, 20, 17, 37));
        second.setPlanQty(new BigDecimal("3202"));

        var result = scheduleService.split(27L, List.of(first, second));

        org.junit.jupiter.api.Assertions.assertTrue(result.isSaved());
        // 合计 6702 与计划数量 7001 不等也应允许保存
        verify(batchMapper, org.mockito.Mockito.times(2)).insert(any());
    }

    /**
     * 存在冲突时保存直接返回冲突明细，不写入数据库
     */
    @Test
    void shouldReturnConflictsAndNotPersistWhenConflictExists() {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(470L);
        order.setWorkOrderCode("GD202611110001");
        order.setBatchQty(new BigDecimal("7001"));
        when(workOrderMapper.selectById(470L)).thenReturn(order);
        when(planMapper.selectList(any())).thenReturn(List.of());
        when(planMapper.selectByWorkOrderIdWithDetails(470L)).thenReturn(List.of());

        ScheduleConflictDto conflict = new ScheduleConflictDto();
        conflict.setType("设备占用");
        conflict.setPlanId(27L);
        conflict.setMessage("设备【灭菌柜】与其他工单重叠");
        when(conflictService.check(any(), any(), any(), any())).thenReturn(List.of(conflict));

        ProcessPlanSaveDto dto = new ProcessPlanSaveDto();
        dto.setPlanId(27L);
        dto.setProcessTypeId(88L);
        dto.setStepOrder(3);
        dto.setPlanQty(new BigDecimal("7001"));
        dto.setPlanStartTime(LocalDateTime.of(2026, 11, 20, 1, 0));
        dto.setPlanEndTime(LocalDateTime.of(2026, 11, 20, 2, 0));
        dto.setRoomId(108);
        dto.setEquipmentId(219);
        ScheduleSaveRequest request = new ScheduleSaveRequest();
        request.setWorkOrderId(470L);
        request.setProcesses(List.of(dto));

        var result = scheduleService.save(470L, request, false);

        org.junit.jupiter.api.Assertions.assertFalse(result.isSaved());
        org.junit.jupiter.api.Assertions.assertEquals(1, result.getConflicts().size());
        verify(planMapper, never()).insert(any());
        verify(planMapper, never()).updateById(any());
        verify(batchMapper, never()).insert(any());
        verify(participantMapper, never()).insert(any());
    }

    // endregion
}
