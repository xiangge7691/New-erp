package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Constants.ProcessExecutionStatus;
import com.tonghui.erp.Common.Dto.Pda.PdaProcessDto;
import com.tonghui.erp.Common.Dto.Pda.PdaTaskDto;
import com.tonghui.erp.Data.Entity.Unit;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch;
import com.tonghui.erp.Data.mapper.BusinessFileMapper;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import com.tonghui.erp.Data.mapper.PersonnelFileMapper;
import com.tonghui.erp.Data.mapper.UnitMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionParticipantMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanBatchMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanMapper;
import com.tonghui.erp.Service.impl.PdaProductionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PDA 任务详情批次展开测试
 * <p>
 * 排产拆分批次后，PDA 按批次展示工序行，不再展示原整道工序行；
 * 未登记实际生产的批次行 executionId 为空，可先调用「确保批次执行记录」再开始计时。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PdaTaskBatchExpansionTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    @Mock
    private WorkOrderProcessExecutionService executionService;
    @Mock
    private WorkOrderProcessExecutionMapper executionMapper;
    @Mock
    private WorkOrderProcessExecutionParticipantMapper participantMapper;
    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private EquipmentMapper equipmentMapper;
    @Mock
    private PersonnelFileMapper personnelFileMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private BusinessFileMapper businessFileMapper;
    @Mock
    private WorkOrderProcessPlanMapper planMapper;
    @Mock
    private WorkOrderProcessPlanBatchMapper planBatchMapper;
    @Mock
    private UnitMapper unitMapper;
    @Mock
    private PerformanceGenerationService performanceGenerationService;

    @InjectMocks
    private PdaProductionServiceImpl pdaProductionService;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 拆分批次后按批次展示两行，原整道工序行不再出现
     */
    @Test
    void shouldExpandBatchesAndHideOriginalProcessRow() {
        stubTask(new ArrayList<>(List.of(originalExecution())));

        PdaTaskDto task = pdaProductionService.getTask(480L);

        assertEquals(2, task.getProcesses().size(), "第 11 道工序拆成两个批次应展示两行");
        assertEquals(2, task.getProcessCount());
        assertEquals(0, task.getDoneCount());
        assertEquals(LocalDateTime.of(2026, 11, 20, 22, 0), task.getPlanStartTime(),
                "工单未填计划开始时间时应取最早排产工序的开始");

        PdaProcessDto first = task.getProcesses().get(0);
        assertEquals(Long.valueOf(146L), first.getPlanBatchId());
        assertEquals(1, first.getBatchNo());
        assertEquals("批次 1", first.getBatchName());
        assertEquals(new BigDecimal("1750"), first.getPlanQty());
        assertEquals("盒", first.getUnitSymbol());
        assertNull(first.getExecutionId(), "未登记实际生产的批次行没有执行记录ID");
        assertEquals("未开始", first.getStatus());
        assertNull(first.getActualQty(), "原工序行的实际数量不应出现在批次行上");

        PdaProcessDto second = task.getProcesses().get(1);
        assertEquals(Long.valueOf(147L), second.getPlanBatchId());
        assertEquals(2, second.getBatchNo());
        assertEquals(new BigDecimal("101"), second.getPlanQty());
        assertNull(second.getExecutionId());
        // 原工序行（实际 8000）被批次行替换
        assertTrue(task.getProcesses().stream().noneMatch(p -> new BigDecimal("8000").equals(p.getActualQty())),
                "不应再展示原整道工序行");
    }

    /**
     * 批次已有执行记录时展示实际数据，另一批次仍为待开始
     */
    @Test
    void shouldShowActualDataForMatchedBatch() {
        WorkOrderProcessExecution matched = new WorkOrderProcessExecution();
        matched.setId(600L);
        matched.setWorkOrderId(480L);
        matched.setPlanId(79L);
        matched.setPlanBatchId(146L);
        matched.setBatchNo(1);
        matched.setProcessTypeId(5L);
        matched.setStepOrder(11);
        matched.setStartTime(LocalDateTime.of(2026, 11, 20, 22, 0));
        matched.setEndTime(LocalDateTime.of(2026, 11, 20, 23, 0));
        matched.setProcessQty(new BigDecimal("1750"));
        matched.setStatus(ProcessExecutionStatus.COMPLETED);
        matched.setPdaSubmitted(1);
        stubTask(new ArrayList<>(List.of(matched)));

        PdaTaskDto task = pdaProductionService.getTask(480L);

        assertEquals(2, task.getProcesses().size());
        PdaProcessDto first = task.getProcesses().get(0);
        assertEquals(Long.valueOf(600L), first.getExecutionId());
        assertEquals(new BigDecimal("1750"), first.getActualQty());
        assertEquals(Boolean.TRUE, first.getDone());
        assertEquals(Boolean.TRUE, first.getSubmitted());

        PdaProcessDto second = task.getProcesses().get(1);
        assertNull(second.getExecutionId());
        assertEquals("未开始", second.getStatus());
    }

    /**
     * 草稿排产不展开批次，仍显示原工序行
     */
    @Test
    void shouldNotExpandDraftPlans() {
        stubTask(new ArrayList<>(List.of(originalExecution())));
        WorkOrderProcessPlan draft = plan();
        draft.setStatus("草稿");
        when(planMapper.selectByWorkOrderIdWithDetails(480L)).thenReturn(List.of(draft));

        PdaTaskDto task = pdaProductionService.getTask(480L);

        assertEquals(1, task.getProcesses().size(), "草稿排产不按批次展开");
        assertEquals(Long.valueOf(560L), task.getProcesses().get(0).getExecutionId());
        assertNull(task.getProcesses().get(0).getPlanBatchId());
    }

    /**
     * 确保批次执行记录：不存在时创建，重复调用不会重复创建
     */
    @Test
    void shouldCreateBatchExecutionWhenAbsent() {
        WorkOrderProcessPlanBatch batch = firstBatch();
        when(planBatchMapper.selectById(146L)).thenReturn(batch);
        when(planMapper.selectById(79L)).thenReturn(plan());
        when(executionService.getOne(any(), eq(false))).thenReturn(null);
        when(executionService.getByWorkOrderId(480L)).thenReturn(new ArrayList<>());

        PdaProcessDto dto = pdaProductionService.ensureBatchExecution(480L, 146L);

        assertEquals(Long.valueOf(146L), dto.getPlanBatchId());
        assertEquals(Long.valueOf(79L), dto.getPlanId());
        assertEquals(1, dto.getBatchNo());
        verify(executionService).save(any(WorkOrderProcessExecution.class));
    }

    // endregion

    // region 测试辅助
    // ===================================
    // 测试辅助
    // ===================================

    /**
     * 构造任务详情桩：工单、计划 79、批次 146/147、单位盒
     */
    private void stubTask(List<WorkOrderProcessExecution> executions) {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(480L);
        order.setWorkOrderCode("GD202611200001");
        order.setBatchQty(new BigDecimal("3500"));
        when(workOrderMapper.selectById(480L)).thenReturn(order);
        when(executionService.getByWorkOrderId(480L)).thenReturn(executions);

        when(planMapper.selectByWorkOrderIdWithDetails(480L)).thenReturn(List.of(plan()));
        when(planBatchMapper.selectByPlanIds(any())).thenReturn(List.of(firstBatch(), secondBatch()));

        Unit unit = new Unit();
        unit.setUnitId(9L);
        unit.setUnitName("盒");
        unit.setSymbol("盒");
        when(unitMapper.selectBatchIds(any())).thenReturn(List.of(unit));
    }

    /**
     * 排产计划：第 11 道工序，拆两个批次
     */
    private WorkOrderProcessPlan plan() {
        WorkOrderProcessPlan plan = new WorkOrderProcessPlan();
        plan.setId(79L);
        plan.setWorkOrderId(480L);
        plan.setProcessTypeId(5L);
        plan.setProcessTypeName("包装");
        plan.setStepOrder(11);
        plan.setStatus("已保存");
        plan.setPlanQty(new BigDecimal("3500"));
        plan.setUnitId(9L);
        plan.setPlanStartTime(LocalDateTime.of(2026, 11, 20, 22, 0));
        plan.setPlanEndTime(LocalDateTime.of(2026, 11, 20, 23, 0));
        return plan;
    }

    /**
     * 批次 1
     */
    private WorkOrderProcessPlanBatch firstBatch() {
        WorkOrderProcessPlanBatch batch = new WorkOrderProcessPlanBatch();
        batch.setId(146L);
        batch.setPlanId(79L);
        batch.setBatchNo(1);
        batch.setBatchName("批次 1");
        batch.setPlanQty(new BigDecimal("1750"));
        batch.setUnitId(9L);
        batch.setPlanStartTime(LocalDateTime.of(2026, 11, 20, 22, 0));
        batch.setPlanEndTime(LocalDateTime.of(2026, 11, 20, 23, 0));
        return batch;
    }

    /**
     * 批次 2
     */
    private WorkOrderProcessPlanBatch secondBatch() {
        WorkOrderProcessPlanBatch batch = new WorkOrderProcessPlanBatch();
        batch.setId(147L);
        batch.setPlanId(79L);
        batch.setBatchNo(2);
        batch.setBatchName("批次 2");
        batch.setPlanQty(new BigDecimal("101"));
        batch.setUnitId(9L);
        return batch;
    }

    /**
     * 原整道工序执行记录（未挂批次）
     */
    private WorkOrderProcessExecution originalExecution() {
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setId(560L);
        execution.setWorkOrderId(480L);
        execution.setProcessTypeId(5L);
        execution.setStepOrder(11);
        execution.setProcessQty(new BigDecimal("8000"));
        execution.setStatus(ProcessExecutionStatus.PENDING);
        return execution;
    }

    // endregion
}
