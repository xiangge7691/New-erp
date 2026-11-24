package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tonghui.erp.Common.Constants.ProcessExecutionStatus;
import com.tonghui.erp.Common.utils.EquipmentUsageConflictValidator;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;
import com.tonghui.erp.Data.mapper.PerformanceRecordMapper;
import com.tonghui.erp.Data.mapper.PreparationProcessTemplateMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionParticipantMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanBatchMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanMapper;
import com.tonghui.erp.Service.impl.WorkOrderProcessExecutionServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 执行记录计划兜底匹配测试
 * <p>
 * 早期 PDA 录入的执行记录没有 plan_id，读取时按「工单+工序类型+工序顺序」
 * 匹配计划并补齐计划数量，保证 PDA 与排产接口口径一致。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderProcessExecutionPlanFallbackTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private WorkOrderProcessExecutionMapper executionMapper;
    @Mock
    private WorkOrderProcessPlanMapper planMapper;
    @Mock
    private WorkOrderProcessPlanBatchMapper planBatchMapper;
    @Mock
    private PreparationProcessTemplateMapper templateMapper;
    @Mock
    private WorkOrderProcessExecutionParticipantMapper participantMapper;
    @Mock
    private PerformanceRecordMapper performanceRecordMapper;
    @Mock
    private EquipmentUsageConflictValidator equipmentUsageConflictValidator;
    @Mock
    private PerformanceGenerationService performanceGenerationService;

    @InjectMocks
    private WorkOrderProcessExecutionServiceImpl service;

    // endregion

    // region 测试准备
    // ===================================
    // 测试准备
    // ===================================

    /**
     * 初始化实体表信息：LambdaQueryWrapper 需要 MyBatis-Plus 的列缓存
     */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, WorkOrderProcessPlan.class);
        TableInfoHelper.initTableInfo(assistant, WorkOrderProcessExecution.class);
    }

    /**
     * 将 Mapper 注入 ServiceImpl 的 baseMapper
     */
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseMapper", executionMapper);
    }

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 未关联计划的执行记录按工序匹配计划并补齐计划数量
     */
    @Test
    void shouldFillPlanByProcessWhenExecutionHasNoPlanId() {
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setId(560L);
        execution.setWorkOrderId(480L);
        execution.setProcessTypeId(5L);
        execution.setStepOrder(11);
        execution.setProcessQty(new BigDecimal("8000"));
        execution.setStartTime(LocalDateTime.of(2026, 11, 20, 8, 0));
        execution.setEndTime(LocalDateTime.of(2026, 11, 20, 10, 0));
        when(executionMapper.selectByWorkOrderIdWithDetails(480L)).thenReturn(List.of(execution));

        WorkOrderProcessPlan plan = new WorkOrderProcessPlan();
        plan.setId(79L);
        plan.setWorkOrderId(480L);
        plan.setProcessTypeId(5L);
        plan.setStepOrder(11);
        plan.setPlanQty(new BigDecimal("3500"));
        plan.setUnitId(9L);
        when(planMapper.selectList(any())).thenReturn(List.of(plan));
        when(participantMapper.selectByExecutionIdsWithPersonnel(any())).thenReturn(List.of());
        when(performanceRecordMapper.selectMaps(any())).thenReturn(List.of());

        List<WorkOrderProcessExecution> result = service.getByWorkOrderId(480L);

        WorkOrderProcessExecution filled = result.get(0);
        assertEquals(Long.valueOf(79L), filled.getPlanId(), "应按工序补齐计划ID");
        assertEquals(new BigDecimal("3500"), filled.getPlanQty(), "计划数量应取排产计划数量");
        assertEquals(ProcessExecutionStatus.COMPLETED, filled.getStatus());
    }

    /**
     * PDA 保存实际记录时把未关联的执行记录挂回排产计划
     */
    @Test
    void shouldPersistPlanReferenceWhenFilling() {
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setId(560L);
        execution.setWorkOrderId(480L);
        execution.setProcessTypeId(5L);
        execution.setStepOrder(11);

        WorkOrderProcessPlan plan = new WorkOrderProcessPlan();
        plan.setId(79L);
        plan.setWorkOrderId(480L);
        plan.setProcessTypeId(5L);
        plan.setStepOrder(11);
        when(planMapper.selectList(any())).thenReturn(List.of(plan));

        service.fillPlanReference(execution);

        assertEquals(Long.valueOf(79L), execution.getPlanId(), "回填后执行记录应带上计划ID");
    }

    // endregion
}
