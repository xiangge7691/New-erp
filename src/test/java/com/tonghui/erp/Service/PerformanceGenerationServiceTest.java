package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.PerformanceCoefficientVersion;
import com.tonghui.erp.Data.Entity.PerformancePersonCoefficient;
import com.tonghui.erp.Data.Entity.PerformanceProcessCoefficient;
import com.tonghui.erp.Data.Entity.PerformanceRecord;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecutionParticipant;
import com.tonghui.erp.Data.mapper.PerformanceCoefficientVersionMapper;
import com.tonghui.erp.Data.mapper.PerformancePersonCoefficientMapper;
import com.tonghui.erp.Data.mapper.PerformanceProcessCoefficientMapper;
import com.tonghui.erp.Data.mapper.PerformanceRecordMapper;
import com.tonghui.erp.Data.mapper.ProcessTypeMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionParticipantMapper;
import com.tonghui.erp.Service.impl.PerformanceGenerationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 绩效生成单元测试
 */
@ExtendWith(MockitoExtension.class)
class PerformanceGenerationServiceTest {

    @Mock
    private WorkOrderProcessExecutionMapper executionMapper;
    @Mock
    private WorkOrderProcessExecutionParticipantMapper participantMapper;
    @Mock
    private PerformanceRecordMapper recordMapper;
    @Mock
    private PerformanceCoefficientVersionMapper versionMapper;
    @Mock
    private PerformanceProcessCoefficientMapper processCoefficientMapper;
    @Mock
    private PerformancePersonCoefficientMapper personCoefficientMapper;
    @Mock
    private ProcessTypeMapper processTypeMapper;

    @InjectMocks
    private PerformanceGenerationServiceImpl generationService;

    /**
     * 按个人工作时间 × 工序系数 × 人员系数生成绩效工时
     */
    @Test
    void shouldGenerateByWorkHoursAndCoefficients() {
        WorkOrderProcessExecution execution = new WorkOrderProcessExecution();
        execution.setId(10L);
        execution.setWorkOrderId(2L);
        execution.setProcessTypeId(5L);
        execution.setPlanId(3L);
        execution.setPlanBatchId(4L);
        execution.setBatchNo(1);
        execution.setStartTime(LocalDateTime.of(2026, 11, 20, 8, 0));
        execution.setEndTime(LocalDateTime.of(2026, 11, 20, 10, 0));
        when(executionMapper.selectById(10L)).thenReturn(execution);

        WorkOrderProcessExecutionParticipant participant = new WorkOrderProcessExecutionParticipant();
        participant.setExecutionId(10L);
        participant.setPersonnelId(7L);
        participant.setWorkHours(new BigDecimal("1.50"));
        participant.setCompletedQty(new BigDecimal("5"));
        when(participantMapper.selectByExecutionIdsWithPersonnel(List.of(10L))).thenReturn(List.of(participant));

        PerformanceCoefficientVersion version = new PerformanceCoefficientVersion();
        version.setId(1L);
        version.setVersionNo("V1");
        version.setStatus("生效");
        when(versionMapper.selectOne(any())).thenReturn(version);

        PerformanceProcessCoefficient processCoef = new PerformanceProcessCoefficient();
        processCoef.setCoefficient(new BigDecimal("1.20"));
        when(processCoefficientMapper.selectOne(any())).thenReturn(processCoef);
        PerformancePersonCoefficient personCoef = new PerformancePersonCoefficient();
        personCoef.setCoefficient(new BigDecimal("1.10"));
        when(personCoefficientMapper.selectOne(any())).thenReturn(personCoef);
        when(recordMapper.selectCount(any())).thenReturn(0L);
        when(recordMapper.insert(any())).thenReturn(1);

        int generated = generationService.generateForExecution(10L);

        assertEquals(1, generated);
        ArgumentCaptor<PerformanceRecord> captor = ArgumentCaptor.forClass(PerformanceRecord.class);
        verify(recordMapper).insert(captor.capture());
        PerformanceRecord record = captor.getValue();
        assertEquals(0, new BigDecimal("1.50").compareTo(record.getWorkHours()));
        assertEquals(0, new BigDecimal("1.98").compareTo(record.getPerformanceHoursTotal()));
        assertEquals(0, new BigDecimal("2.00").compareTo(record.getProcessActualHours()));
        assertEquals(java.time.LocalDate.of(2026, 11, 20), record.getPerformanceDate());
        assertEquals("系统生成", record.getSource());
    }
}
