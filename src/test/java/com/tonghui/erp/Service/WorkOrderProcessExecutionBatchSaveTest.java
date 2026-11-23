package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tonghui.erp.Common.utils.EquipmentUsageConflictValidator;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecutionParticipant;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlan;
import com.tonghui.erp.Data.Entity.WorkOrderProcessPlanBatch;
import com.tonghui.erp.Data.mapper.PerformanceRecordMapper;
import com.tonghui.erp.Data.mapper.PreparationProcessTemplateMapper;
import com.tonghui.erp.Data.mapper.WorkOrderMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionParticipantMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanBatchMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessPlanMapper;
import com.tonghui.erp.Service.PerformanceGenerationService;
import com.tonghui.erp.Service.impl.WorkOrderProcessExecutionServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工单工序执行记录批量保存单元测试
 * <p>
 * 覆盖「实际生产」只回传单个工序/批次时的合并语义：
 * 缺失字段从排产批次、工序计划与制剂模板补齐，且不删除其他工序的执行记录。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderProcessExecutionBatchSaveTest {

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
     * 初始化实体表信息，让批量保存中的 Lambda 更新能解析字段
     */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
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
     * 只保存拆分批次的执行行：应补齐工序顺序、配置室、设备与关键工艺参数，
     * 只替换同一工序的旧行，其他工序记录保持不变。
     */
    @Test
    void shouldEnrichBatchRowAndKeepOtherProcessExecutions() {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(470L);
        order.setPreparationId(244L);
        order.setIsDeleted(0);
        when(workOrderMapper.selectById(470L)).thenReturn(order);

        // 既有执行记录：第 1 道工序 + 第 3 道工序（灭菌的整道工序行）
        WorkOrderProcessExecution first = new WorkOrderProcessExecution();
        first.setId(473L);
        first.setWorkOrderId(470L);
        first.setProcessTypeId(189L);
        first.setStepOrder(1);
        first.setRoomId(104);
        first.setEquipmentId(203);
        first.setKeyProcessParams("60~70℃");
        first.setLocked(0);

        WorkOrderProcessExecution sterilization = new WorkOrderProcessExecution();
        sterilization.setId(475L);
        sterilization.setWorkOrderId(470L);
        sterilization.setProcessTypeId(88L);
        sterilization.setStepOrder(3);
        sterilization.setRoomId(108);
        sterilization.setEquipmentId(219);
        sterilization.setKeyProcessParams("121℃，灭菌20min");
        sterilization.setLocked(0);
        when(executionMapper.selectList(any())).thenReturn(List.of(first, sterilization));

        // 拆分后的批次（工序计划 27 -> 第 3 道工序 灭菌）
        WorkOrderProcessPlanBatch batch = new WorkOrderProcessPlanBatch();
        batch.setId(45L);
        batch.setPlanId(27L);
        batch.setBatchNo(1);
        batch.setRoomId(108);
        batch.setEquipmentId(219);
        batch.setUnitId(2L);
        batch.setPlanQty(new BigDecimal("3500"));
        when(planBatchMapper.selectById(45L)).thenReturn(batch);

        WorkOrderProcessPlan plan = new WorkOrderProcessPlan();
        plan.setId(27L);
        plan.setProcessTypeId(88L);
        plan.setStepOrder(3);
        plan.setRoomId(108);
        plan.setEquipmentId(219);
        plan.setPlanQty(new BigDecimal("7001"));
        when(planMapper.selectById(27L)).thenReturn(plan);

        PreparationProcessTemplate template = new PreparationProcessTemplate();
        template.setKeyProcessParams("121℃，灭菌20min");
        when(templateMapper.selectOne(any())).thenReturn(template);

        // 「实际生产」只回传批次定位字段
        WorkOrderProcessExecution incoming = new WorkOrderProcessExecution();
        incoming.setPlanId(27L);
        incoming.setPlanBatchId(45L);
        incoming.setProcessTypeId(88L);

        service.batchSave(470L, List.of(incoming));

        ArgumentCaptor<WorkOrderProcessExecution> captor =
                ArgumentCaptor.forClass(WorkOrderProcessExecution.class);
        verify(executionMapper).insert(captor.capture());
        WorkOrderProcessExecution saved = captor.getValue();

        assertEquals(3, saved.getStepOrder());
        assertEquals(1, saved.getBatchNo());
        assertEquals(108, saved.getRoomId());
        assertEquals(219, saved.getEquipmentId());
        assertEquals(new BigDecimal("3500"), saved.getProcessQty());
        assertEquals("121℃，灭菌20min", saved.getKeyProcessParams());
        assertEquals(2L, saved.getUnitId());

        // 仅替换同工序（灭菌）的旧行，第 1 道工序保留
        verify(executionMapper).physicalDeleteById(475L);
        verify(executionMapper, never()).physicalDeleteById(473L);
    }

    /**
     * 前端「实际生产」按排产详情回传的字段（executionId / actualStartTime / actualEndTime / actualQty）
     * 应能正确反序列化为执行记录
     */
    @Test
    void shouldAcceptActualScheduleFieldsFromFrontend() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String json = "[{\"executionId\":529,\"planId\":49,\"processTypeId\":63,\"stepOrder\":1,"
                + "\"actualStartTime\":\"2026-11-19T18:31:22\","
                + "\"actualEndTime\":\"2026-11-20T18:31:24\",\"actualQty\":5000,"
                + "\"participants\":[{\"personnelId\":79,\"personnelName\":\"\",\"isPrimary\":0}]}]";

        List<WorkOrderProcessExecution> rows =
                mapper.readValue(json, new TypeReference<List<WorkOrderProcessExecution>>() { });

        WorkOrderProcessExecution row = rows.get(0);
        assertEquals(529L, row.getId());
        assertEquals(LocalDateTime.of(2026, 11, 19, 18, 31, 22), row.getStartTime());
        assertEquals(LocalDateTime.of(2026, 11, 20, 18, 31, 24), row.getEndTime());
        assertEquals(0, row.getProcessQty().compareTo(new BigDecimal("5000")));
        assertEquals(79L, row.getParticipants().get(0).getPersonnelId());
    }

    /**
     * 参与人未填工作时间与完成数量时：工作时间按工序实际起止计算，完成数量按人数均分
     */
    @Test
    void shouldDeriveParticipantHoursAndQtyWhenFrontendOmitsThem() {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(470L);
        order.setIsDeleted(0);
        when(workOrderMapper.selectById(470L)).thenReturn(order);
        when(executionMapper.selectList(any())).thenReturn(List.of());

        WorkOrderProcessExecution incoming = new WorkOrderProcessExecution();
        incoming.setPlanId(49L);
        incoming.setProcessTypeId(63L);
        incoming.setStepOrder(1);
        incoming.setStartTime(LocalDateTime.of(2026, 11, 19, 18, 31, 22));
        incoming.setEndTime(LocalDateTime.of(2026, 11, 20, 18, 31, 24));
        incoming.setProcessQty(new BigDecimal("5000"));
        incoming.setParticipants(java.util.stream.IntStream.range(0, 5)
                .mapToObj(i -> {
                    WorkOrderProcessExecutionParticipant participant =
                            new WorkOrderProcessExecutionParticipant();
                    participant.setPersonnelId(71L + i);
                    participant.setIsPrimary(0);
                    return participant;
                })
                .toList());

        when(participantMapper.selectCount(any())).thenReturn(5L);

        service.batchSave(470L, List.of(incoming));

        ArgumentCaptor<WorkOrderProcessExecutionParticipant> captor =
                ArgumentCaptor.forClass(WorkOrderProcessExecutionParticipant.class);
        verify(participantMapper, org.mockito.Mockito.times(5)).insert(captor.capture());
        BigDecimal qtySum = BigDecimal.ZERO;
        for (WorkOrderProcessExecutionParticipant participant : captor.getAllValues()) {
            assertEquals(0, participant.getWorkHours().compareTo(new BigDecimal("24")));
            assertEquals(0, participant.getCompletedQty().compareTo(new BigDecimal("1000")));
            qtySum = qtySum.add(participant.getCompletedQty());
        }
        assertEquals(0, qtySum.compareTo(new BigDecimal("5000")));
    }

    /**
     * 新完工（本次由未完工变为已完成）但没有参与人：应拒绝整批保存，避免「已完成但无绩效」脏数据
     */
    @Test
    void shouldRejectNewlyCompletedRowWithoutParticipants() {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(480L);
        order.setIsDeleted(0);
        when(workOrderMapper.selectById(480L)).thenReturn(order);

        WorkOrderProcessExecution existing = new WorkOrderProcessExecution();
        existing.setId(550L);
        existing.setWorkOrderId(480L);
        existing.setProcessTypeId(189L);
        existing.setStepOrder(1);
        existing.setStatus("待执行");
        existing.setLocked(0);
        when(executionMapper.selectList(any())).thenReturn(List.of(existing));
        when(participantMapper.selectCount(any())).thenReturn(0L);

        WorkOrderProcessExecution incoming = new WorkOrderProcessExecution();
        incoming.setId(550L);
        incoming.setStartTime(LocalDateTime.of(2026, 11, 20, 0, 0));
        incoming.setEndTime(LocalDateTime.of(2026, 11, 20, 1, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.batchSave(480L, List.of(incoming)));
        assertEquals("工序【1】未登记参与人，无法生成绩效；请先登记参与人", ex.getMessage());
        verify(performanceGenerationService, never()).generateForExecution(any());
    }

    /**
     * 新完工且已登记参与人：保存后按执行记录生成绩效
     */
    @Test
    void shouldGeneratePerformanceWhenCompletedRowHasParticipants() {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(480L);
        order.setIsDeleted(0);
        when(workOrderMapper.selectById(480L)).thenReturn(order);
        when(executionMapper.selectList(any())).thenReturn(List.of());
        when(participantMapper.selectCount(any())).thenReturn(1L);

        WorkOrderProcessExecutionParticipant participant = new WorkOrderProcessExecutionParticipant();
        participant.setPersonnelId(71L);

        WorkOrderProcessExecution incoming = new WorkOrderProcessExecution();
        incoming.setPlanId(69L);
        incoming.setProcessTypeId(189L);
        incoming.setStepOrder(1);
        incoming.setStartTime(LocalDateTime.of(2026, 11, 20, 0, 0));
        incoming.setEndTime(LocalDateTime.of(2026, 11, 20, 1, 0));
        incoming.setParticipants(List.of(participant));

        service.batchSave(480L, List.of(incoming));

        verify(performanceGenerationService).generateForExecution(any());
    }

    /**
     * 历史已完成但无参与人的记录再次保存时不阻断（便于补登参与人后重新触发），也不生成绩效
     */
    @Test
    void shouldAllowResavingAlreadyCompletedRowWithoutParticipants() {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(480L);
        order.setIsDeleted(0);
        when(workOrderMapper.selectById(480L)).thenReturn(order);

        WorkOrderProcessExecution existing = new WorkOrderProcessExecution();
        existing.setId(550L);
        existing.setWorkOrderId(480L);
        existing.setProcessTypeId(189L);
        existing.setStepOrder(1);
        existing.setStatus("已完成");
        existing.setLocked(0);
        when(executionMapper.selectList(any())).thenReturn(List.of(existing));
        when(participantMapper.selectCount(any())).thenReturn(0L);

        WorkOrderProcessExecution incoming = new WorkOrderProcessExecution();
        incoming.setId(550L);
        incoming.setStartTime(LocalDateTime.of(2026, 11, 20, 0, 0));
        incoming.setEndTime(LocalDateTime.of(2026, 11, 20, 1, 0));

        assertDoesNotThrow(() -> service.batchSave(480L, List.of(incoming)));
        verify(performanceGenerationService, never()).generateForExecution(any());
    }

    // endregion
}
