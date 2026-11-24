package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PageRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 排产/班组/绩效查询冒烟测试：确认迁移后的库表与实体、SQL 映射一致
 */
@SpringBootTest
class SchedulingQuerySmokeTest {

    @Autowired
    private WorkOrderScheduleService scheduleService;

    @Autowired
    private ProductionTeamService teamService;

    @Autowired
    private PerformanceService performanceService;

    @Autowired
    private WorkOrderProcessExecutionService executionService;

    @Test
    void queriesShouldRunAgainstTestDatabase() {
        assertDoesNotThrow(() -> scheduleService.listTasks(null, null, null, null));
        assertDoesNotThrow(() -> scheduleService.gantt("task",
                LocalDate.now().minusDays(30).atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay(),
                null, null, null, null, null));
        assertDoesNotThrow(() -> teamService.listActive());
        assertDoesNotThrow(() -> executionService.listCompleted(null, null, null));

        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(0);
        pageRequest.setPageSize(20);
        assertDoesNotThrow(() -> performanceService.query(
                LocalDate.now().minusMonths(1), LocalDate.now(), null, null, null,
                null, null, null, "排产冒烟", pageRequest));
    }
}
