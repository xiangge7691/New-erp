package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tonghui.erp.Common.Dto.Performance.PerformanceManualRequest;
import com.tonghui.erp.Data.Entity.PerformanceRecord;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.WorkOrderProcessExecution;
import com.tonghui.erp.Data.mapper.PerformanceRecordMapper;
import com.tonghui.erp.Data.mapper.PersonnelFileMapper;
import com.tonghui.erp.Data.mapper.WorkOrderProcessExecutionMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 手动补录按实际执行记录关联：未排产工单也能补录
 */
@SpringBootTest
class ManualPerformanceFromExecutionTest {

    @Autowired
    private PerformanceService performanceService;

    @Autowired
    private WorkOrderProcessExecutionMapper executionMapper;

    @Autowired
    private PersonnelFileMapper personnelFileMapper;

    @Autowired
    private PerformanceRecordMapper recordMapper;

    @Test
    @Transactional
    void manualShouldLinkToCompletedExecutionWithoutPlan() {
        WorkOrderProcessExecution execution = executionMapper.selectOne(
                new LambdaQueryWrapper<WorkOrderProcessExecution>()
                        .eq(WorkOrderProcessExecution::getIsDeleted, 0)
                        .isNotNull(WorkOrderProcessExecution::getEndTime)
                        .orderByDesc(WorkOrderProcessExecution::getEndTime)
                        .last("LIMIT 1"));
        Assumptions.assumeTrue(execution != null, "测试库没有已完工执行记录");

        List<PersonnelFile> personnel = personnelFileMapper.selectList(
                new LambdaQueryWrapper<PersonnelFile>().eq(PersonnelFile::getStatus, 1));
        PersonnelFile person = personnel.stream()
                .filter(p -> recordMapper.selectCount(new LambdaQueryWrapper<PerformanceRecord>()
                        .eq(PerformanceRecord::getExecutionId, execution.getId())
                        .eq(PerformanceRecord::getPersonnelId, p.getPersonnelFileId())
                        .eq(PerformanceRecord::getRecordStatus, 1)) == 0)
                .findFirst().orElse(null);
        Assumptions.assumeTrue(person != null, "没有可用于补录的人员");

        PerformanceManualRequest request = new PerformanceManualRequest();
        request.setPersonnelId(person.getPersonnelFileId());
        request.setPerformanceDate(LocalDate.now());
        request.setCalcMode("按系数计算");
        request.setPerformanceHours(new BigDecimal("1.5"));
        request.setNote("按执行记录补录测试");
        request.setWorkOrderId(execution.getWorkOrderId());
        request.setProcessTypeId(execution.getProcessTypeId());
        request.setAllowDuplicate(true);

        PerformanceRecord record = performanceService.manual(request);

        assertNotNull(record.getId());
        assertEquals(execution.getWorkOrderId(), record.getWorkOrderId());
        assertEquals(execution.getProcessTypeId(), record.getProcessTypeId());
        assertEquals(execution.getId(), record.getExecutionId());
    }
}
