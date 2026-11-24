package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tonghui.erp.Common.Dto.Scheduling.PlanBatchDto;
import com.tonghui.erp.Common.Dto.Scheduling.ProcessPlanSaveDto;
import com.tonghui.erp.Common.Dto.Scheduling.ScheduleSaveRequest;
import com.tonghui.erp.Data.Entity.WorkOrder;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 排产保存批次保留单元测试
 * <p>
 * 覆盖保存排产时就地更新既有批次（不重建批次ID）的语义：
 * 优先按批次ID匹配，未带ID时按批次序号匹配；未提交的批次删除；
 * 已登记实际生产的批次拒绝删除，避免执行记录的 plan_batch_id 悬挂。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderScheduleBatchPreserveTest {

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
        TableInfoHelper.initTableInfo(assistant, WorkOrderProcessPlanBatch.class);
    }

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 保存排产时既有批次就地更新：带ID按ID匹配，未带ID按批次序号匹配，未提交的批次删除
     */
    @Test
    void shouldUpdateExistingBatchesInsteadOfRecreating() {
        stubSaveContext();
        WorkOrderProcessPlanBatch first = batch(146L, 1, "计划中");
        WorkOrderProcessPlanBatch second = batch(147L, 2, "已完工");
        WorkOrderProcessPlanBatch removed = batch(148L, 3, "计划中");
        when(batchMapper.selectByPlanIds(any())).thenReturn(List.of(first, second, removed));
        when(executionMapper.selectCount(any())).thenReturn(0L);

        // 批次1 带ID回传，批次2 只带序号
        PlanBatchDto withId = batchDto(146L, 1);
        PlanBatchDto withNo = batchDto(null, 2);
        scheduleService.save(470L, request(withId, withNo), false);

        // 既有批次按ID就地更新，状态沿用存储值（已完工不会被重置）
        verify(batchMapper).updateById(argThat((WorkOrderProcessPlanBatch b) ->
                Long.valueOf(146L).equals(b.getId()) && "计划中".equals(b.getStatus())));
        verify(batchMapper).updateById(argThat((WorkOrderProcessPlanBatch b) ->
                Long.valueOf(147L).equals(b.getId()) && "已完工".equals(b.getStatus())));
        // 未提交的批次物理删除
        verify(batchMapper).physicalDeleteById(148L);
        // 不再整体删除批次后重建
        verify(batchMapper, never()).physicalDeleteByPlanId(anyLong());
        verify(batchMapper, never()).insert(any());
    }

    /**
     * 未提交的批次已登记实际生产时拒绝保存，避免执行记录悬挂
     */
    @Test
    void shouldRejectDeletingBatchWithExecutions() {
        stubSaveContext();
        WorkOrderProcessPlanBatch first = batch(146L, 1, "计划中");
        WorkOrderProcessPlanBatch removed = batch(148L, 3, "计划中");
        when(batchMapper.selectByPlanIds(any())).thenReturn(List.of(first, removed));
        when(executionMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> scheduleService.save(470L, request(batchDto(146L, 1)), false));

        verify(batchMapper, never()).physicalDeleteById(148L);
        org.junit.jupiter.api.Assertions.assertTrue(
                error.getMessage().contains("已登记实际生产"), "应提示批次已登记实际生产");
    }

    // endregion

    // region 测试辅助
    // ===================================
    // 测试辅助
    // ===================================

    /**
     * 构造保存排产所需的工单、既有计划与冲突校验桩
     */
    private void stubSaveContext() {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(470L);
        order.setWorkOrderCode("GD202611110001");
        order.setBatchQty(new BigDecimal("7001"));
        when(workOrderMapper.selectById(470L)).thenReturn(order);

        WorkOrderProcessPlan plan = new WorkOrderProcessPlan();
        plan.setId(27L);
        plan.setWorkOrderId(470L);
        plan.setProcessTypeId(88L);
        plan.setStepOrder(3);
        when(planMapper.selectList(any())).thenReturn(List.of(plan));
        when(planMapper.selectByWorkOrderIdWithDetails(470L)).thenReturn(List.of());
        when(conflictService.check(any(), any(), any(), any())).thenReturn(List.of());
    }

    /**
     * 构造排产保存请求
     */
    private ScheduleSaveRequest request(PlanBatchDto... batches) {
        ProcessPlanSaveDto dto = new ProcessPlanSaveDto();
        dto.setPlanId(27L);
        dto.setProcessTypeId(88L);
        dto.setStepOrder(3);
        dto.setPlanQty(new BigDecimal("7001"));
        dto.setBatches(List.of(batches));
        ScheduleSaveRequest request = new ScheduleSaveRequest();
        request.setWorkOrderId(470L);
        request.setProcesses(List.of(dto));
        return request;
    }

    /**
     * 构造批次 DTO
     */
    private PlanBatchDto batchDto(Long id, Integer batchNo) {
        PlanBatchDto dto = new PlanBatchDto();
        dto.setId(id);
        dto.setBatchNo(batchNo);
        dto.setBatchName("批次 " + batchNo);
        dto.setPlanQty(new BigDecimal("100"));
        return dto;
    }

    /**
     * 构造既有批次实体
     */
    private WorkOrderProcessPlanBatch batch(Long id, Integer batchNo, String status) {
        WorkOrderProcessPlanBatch batch = new WorkOrderProcessPlanBatch();
        batch.setId(id);
        batch.setPlanId(27L);
        batch.setBatchNo(batchNo);
        batch.setBatchName("批次 " + batchNo);
        batch.setStatus(status);
        return batch;
    }

    // endregion
}
