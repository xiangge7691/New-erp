package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.InspectionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * 存量请检单补建检验任务（一次性运维工具，不在常规测试中执行）
 * <p>
 * 使用方式：./mvnw test -Dtest=InspectionTaskBackfillRunner
 * 逻辑：找出未关联检验任务的请检单，逐条调用 createTaskFromRequest 补建任务。
 * 已有关联任务、或既无工单又被检品属性不合法的请检单会被自动跳过，可重复执行。
 * </p>
 */
@SpringBootTest
public class InspectionTaskBackfillRunner {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    @Autowired
    private InspectionRequestService inspectionRequestService;

    @Autowired
    private InspectionTaskService inspectionTaskService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // endregion

    // region 补数
    // ===================================
    // 补数
    // ===================================

    /**
     * 为存量请检单补建检验任务
     */
    @Test
    public void backfillTasksForExistingRequests() {
        List<Long> requestIds = jdbcTemplate.queryForList(
                "SELECT r.id FROM inspection_request r "
                        + "LEFT JOIN inspection_task t ON t.request_ref_id = r.id AND t.is_deleted = 0 "
                        + "WHERE r.is_deleted = 0 AND t.id IS NULL ORDER BY r.id", Long.class);
        int created = 0;
        int skipped = 0;
        for (Long requestId : requestIds) {
            InspectionRequest request = inspectionRequestService.getById(requestId);
            if (request == null) {
                continue;
            }
            if (inspectionTaskService.createTaskFromRequest(request) == null) {
                skipped++;
            } else {
                created++;
            }
        }
        System.out.println("[Backfill] 待补请检单 " + requestIds.size()
                + " 条，生成任务 " + created + " 条，跳过 " + skipped + " 条");
    }

    // endregion
}
