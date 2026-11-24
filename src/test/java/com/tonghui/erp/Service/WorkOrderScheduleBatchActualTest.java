package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Constants.PlanBatchStatus;
import com.tonghui.erp.Common.Constants.ProcessExecutionStatus;
import com.tonghui.erp.Common.Dto.Scheduling.PlanBatchDto;
import com.tonghui.erp.Common.Dto.Scheduling.ProcessScheduleDto;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleDetailDto;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 排产详情批次实际状态单元测试
 * <p>
 * 拆分后的批次行必须返回实际状态：未登记实际生产时为「待执行」，
 * 存在多条执行记录时按最早开始、最晚结束、数量合计汇总。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderScheduleBatchActualTest {

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

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 未登记实际生产的批次也要返回实际状态「待执行」
     */
    @Test
    void shouldReturnPendingActualStatusWhenBatchHasNoExecution() {
        stubDetail(List.of());
        List<PlanBatchDto> batches = loadBatches();

        assertEquals(ProcessExecutionStatus.PENDING, batches.get(0).getActualStatus(),
                "无执行记录的批次实际状态应为待执行");
        assertNull(batches.get(0).getActualQty(), "无执行记录时实际数量应为空");
        assertNull(batches.get(0).getExecutionId(), "无执行记录时执行记录ID应为空");
        assertNull(batches.get(0).getLocked(), "无执行记录时锁定标识应为空");
        assertEquals(PlanBatchStatus.PLANNED, batches.get(0).getStatus(), "无执行记录时批次状态为计划中");
    }

    /**
     * 一个批次有多条执行记录时按最早开始、最晚结束、数量合计汇总
     */
    @Test
    void shouldAggregateMultipleExecutionsForBatch() {
        WorkOrderProcessExecution completed = execution(1L, 146L,
                LocalDateTime.of(2026, 11, 20, 8, 0), LocalDateTime.of(2026, 11, 20, 10, 0),
                new BigDecimal("1750"), ProcessExecutionStatus.COMPLETED, 1);
        WorkOrderProcessExecution inProgress = execution(2L, 146L,
                LocalDateTime.of(2026, 11, 20, 11, 0), null,
                new BigDecimal("500"), ProcessExecutionStatus.IN_PROGRESS, 0);
        stubDetail(List.of(completed, inProgress));

        List<PlanBatchDto> batches = loadBatches();
        PlanBatchDto first = batches.get(0);

        assertEquals(ProcessExecutionStatus.IN_PROGRESS, first.getActualStatus(),
                "存在执行中的记录时批次实际状态应为执行中");
        assertEquals(new BigDecimal("2250"), first.getActualQty(), "实际数量应为多条记录合计");
        assertEquals(LocalDateTime.of(2026, 11, 20, 8, 0), first.getActualStartTime(), "实际开始取最早");
        assertEquals(LocalDateTime.of(2026, 11, 20, 10, 0), first.getActualEndTime(), "实际结束取最晚");
        assertNull(first.getExecutionId(), "多条执行记录时不返回单条执行ID");
        assertEquals(1, first.getLocked(), "任一条已锁定即视为锁定");
        assertEquals(ProcessExecutionStatus.PENDING, batches.get(1).getActualStatus(),
                "另一批次没有执行记录应为待执行");
    }

    // endregion

    // region 测试辅助
    // ===================================
    // 测试辅助
    // ===================================

    /**
     * 构造详情所需工单、计划、批次与执行记录桩
     */
    private void stubDetail(List<WorkOrderProcessExecution> executions) {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(480L);
        order.setWorkOrderCode("GD202611200001");
        order.setBatchQty(new BigDecimal("3500"));
        when(workOrderMapper.selectById(480L)).thenReturn(order);

        WorkOrderProcessPlan plan = new WorkOrderProcessPlan();
        plan.setId(79L);
        plan.setWorkOrderId(480L);
        plan.setProcessTypeId(5L);
        plan.setStepOrder(11);
        plan.setPlanQty(new BigDecimal("3500"));
        plan.setUnitId(9L);
        when(planMapper.selectByWorkOrderIdWithDetails(480L)).thenReturn(List.of(plan));

        WorkOrderProcessPlanBatch first = batch(146L, 1);
        WorkOrderProcessPlanBatch second = batch(147L, 2);
        when(batchMapper.selectByPlanIds(any())).thenReturn(List.of(first, second));
        when(participantMapper.selectByPlanIdsWithPersonnel(any())).thenReturn(List.of());
        when(executionMapper.selectByWorkOrderIdWithDetails(480L)).thenReturn(executions);
        when(conflictService.check(any(), any(), any())).thenReturn(List.of());
    }

    /**
     * 查询详情并返回第 11 道工序的批次
     */
    private List<PlanBatchDto> loadBatches() {
        ScheduleDetailDto detail = scheduleService.getDetail(480L);
        ProcessScheduleDto process = detail.getProcesses().get(0);
        return process.getBatches();
    }

    /**
     * 构造批次实体
     */
    private WorkOrderProcessPlanBatch batch(Long id, Integer batchNo) {
        WorkOrderProcessPlanBatch batch = new WorkOrderProcessPlanBatch();
        batch.setId(id);
        batch.setPlanId(79L);
        batch.setBatchNo(batchNo);
        batch.setBatchName("批次 " + batchNo);
        batch.setPlanQty(new BigDecimal("1750"));
        batch.setUnitId(9L);
        return batch;
    }

    /**
     * 构造执行记录
     */
    private WorkOrderProcessExecution execution(Long id, Long planBatchId,
                                                LocalDateTime start, LocalDateTime end,
                                                BigDecimal qty, String status, Integer locked) {
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setId(id);
        execution.setWorkOrderId(480L);
        execution.setPlanId(79L);
        execution.setPlanBatchId(planBatchId);
        execution.setStepOrder(11);
        execution.setProcessTypeId(5L);
        execution.setStartTime(start);
        execution.setEndTime(end);
        execution.setProcessQty(qty);
        execution.setStatus(status);
        execution.setLocked(locked);
        return execution;
    }

    // endregion
}
