package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Constants.ProcessExecutionStatus;
import com.tonghui.erp.Common.Dto.Pda.PdaProcessDto;
import com.tonghui.erp.Common.Dto.Pda.PdaTaskDto;
import com.tonghui.erp.Data.Entity.WorkOrder;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * PDA 生产任务计划数量口径测试
 * <p>
 * 计划数量只取排产计划/批次；取不到时返回空，不能回退成实际加工数量。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PdaProductionPlanQtyTest {

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
     * 有计划的工序返回计划数量；无计划关联的工序返回空，不倒填实际数量
     */
    @Test
    void shouldNotUseActualQtyAsPlanQty() {
        WorkOrder order = new WorkOrder();
        order.setWorkOrderId(480L);
        order.setWorkOrderCode("GD202611200001");
        order.setBatchQty(new BigDecimal("3500"));
        when(workOrderMapper.selectById(480L)).thenReturn(order);

        // 第 2 道：已关联计划，计划 3500、实际 3500
        WorkOrderProcessExecution linked = execution(551L, 2, new BigDecimal("3500"));
        linked.setPlanQty(new BigDecimal("3500"));
        // 第 11 道：未关联计划，实际 8000，计划数量应为空而不是 8000
        WorkOrderProcessExecution unlinked = execution(560L, 11, new BigDecimal("8000"));
        when(executionService.getByWorkOrderId(480L))
                .thenReturn(new ArrayList<>(List.of(linked, unlinked)));

        PdaTaskDto task = pdaProductionService.getTask(480L);
        PdaProcessDto step2 = task.getProcesses().get(0);
        PdaProcessDto step11 = task.getProcesses().get(1);

        assertEquals(new BigDecimal("3500"), step2.getPlanQty(), "有关联计划时应返回计划数量");
        assertEquals(new BigDecimal("3500"), step2.getActualQty(), "实际数量取自加工数量");
        assertNull(step11.getPlanQty(), "无计划关联时计划数量应为空，不能回填实际数量");
        assertEquals(new BigDecimal("8000"), step11.getActualQty(), "实际数量照常返回");
    }

    // endregion

    // region 测试辅助
    // ===================================
    // 测试辅助
    // ===================================

    /**
     * 构造执行记录
     */
    private WorkOrderProcessExecution execution(Long id, Integer stepOrder, BigDecimal qty) {
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setId(id);
        execution.setWorkOrderId(480L);
        execution.setStepOrder(stepOrder);
        execution.setProcessTypeId(stepOrder == 11 ? 5L : 63L);
        execution.setProcessQty(qty);
        execution.setStatus(ProcessExecutionStatus.COMPLETED);
        execution.setStartTime(java.time.LocalDateTime.of(2026, 11, 20, 8, 0));
        execution.setEndTime(java.time.LocalDateTime.of(2026, 11, 20, 10, 0));
        return execution;
    }

    // endregion
}
