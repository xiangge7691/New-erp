package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tonghui.erp.Common.utils.EquipmentUsageConflictValidator;
import com.tonghui.erp.Common.utils.WorkOrderExecutionTimeValidator;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecutionParticipant;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工序/批次完工单元测试
 * <p>
 * 覆盖「保存只登记开始、完工单独登记结束」的契约：
 * 显式结束时间必须晚于开始时间；未传结束时间时自动取当前时间并锁定、生成绩效。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderProcessExecutionCompleteTest {

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
    private WorkOrderExecutionTimeValidator executionTimeValidator;
    @Mock
    private PerformanceGenerationService performanceGenerationService;
    @Mock
    private BusinessFileService businessFileService;

    @InjectMocks
    private WorkOrderProcessExecutionServiceImpl service;

    // endregion

    // region 测试准备
    // ===================================
    // 测试准备
    // ===================================

    /**
     * 初始化实体表信息，让完工流程中的 Lambda 更新能解析字段
     */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, WorkOrderProcessExecution.class);
        TableInfoHelper.initTableInfo(assistant, WorkOrderProcessExecutionParticipant.class);
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
     * 显式传入的结束时间早于开始时间 → 拒绝，且不落库
     */
    @Test
    void shouldRejectExplicitEndBeforeStart() {
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setId(1L);
        execution.setWorkOrderId(470L);
        execution.setStepOrder(1);
        execution.setStartTime(LocalDateTime.of(2026, 11, 20, 10, 0));
        execution.setLocked(0);
        when(executionMapper.selectById(1L)).thenReturn(execution);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.complete(1L, LocalDateTime.of(2026, 11, 20, 9, 0)));
        assertTrue(ex.getMessage().contains("必须晚于实际开始时间"), ex.getMessage());
    }

    /**
     * 未传结束时间 → 取当前时间，锁定记录、设置完成时间并生成绩效
     */
    @Test
    void shouldFillCurrentTimeAndLockWhenEndAbsent() {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setId(1L);
        execution.setWorkOrderId(470L);
        execution.setProcessTypeId(88L);
        execution.setStepOrder(1);
        execution.setStartTime(start);
        execution.setLocked(0);
        when(executionMapper.selectById(1L)).thenReturn(execution);

        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(470L);
        order.setConfigDate(start.minusDays(1));
        when(workOrderMapper.selectById(470L)).thenReturn(order);

        when(participantMapper.selectCount(any())).thenReturn(1L);
        when(participantMapper.selectByExecutionIdsWithPersonnel(any())).thenReturn(List.of());

        WorkOrderProcessExecution result = service.complete(1L, null);

        assertNotNull(result.getEndTime(), "未传结束时间时应自动补当前时间");
        assertTrue(result.getEndTime().isAfter(start), "结束时间必须晚于开始时间");
        assertEquals(Integer.valueOf(1), result.getLocked());
        assertEquals("已完成", result.getStatus());
        assertNotNull(result.getCompletedTime());
        verify(performanceGenerationService).generateForExecution(1L);
    }

    // endregion
}
