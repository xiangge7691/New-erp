package com.tonghui.erp.Service;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

/**
 * 检验任务存量数据回退工具（一次性运维工具，不在常规测试中执行）
 * <p>
 * 使用方式：./mvnw test -Dtest=InspectionTaskReopenRunner
 * 逻辑：找出“已完成但缺少留样/销毁记录”的检验任务，退回留样或销毁环节，
 * 同步六步状态，使前端可以重新办理留样/销毁并上传附件。脚本可重复执行。
 * </p>
 */
@SpringBootTest
public class InspectionTaskReopenRunner {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(InspectionTaskReopenRunner.class);

    // endregion

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // endregion

    // region 回退任务
    // ===================================
    // 回退任务
    // ===================================

    /**
     * 回退被旧逻辑自动置为已完成的检验任务
     */
    @Test
    public void reopenAutoDoneTasks() {
        List<Map<String, Object>> tasks = findAffectedTasks();
        if (tasks.isEmpty()) {
            log.info("没有需要回退的检验任务");
            return;
        }
        log.info("发现 {} 条需要回退的检验任务：", tasks.size());
        for (Map<String, Object> task : tasks) {
            log.info("  id={}, taskCode={}, itemCategory={}, retentionRequired={}, destructionRequired={}, "
                            + "retentionRefId={}, destructionRefId={}",
                    task.get("id"), task.get("task_code"), task.get("item_category"),
                    task.get("retention_required"), task.get("destruction_required"),
                    task.get("retention_ref_id"), task.get("destruction_ref_id"));
        }
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Integer reopened = transactionTemplate.execute(status -> reopen(tasks));
        log.info("存量检验任务回退完成，回退 {} 条", reopened);
    }

    /**
     * 查询已完成但缺少留样/销毁记录的任务
     */
    private List<Map<String, Object>> findAffectedTasks() {
        return jdbcTemplate.queryForList(
                "SELECT id, task_code, item_category, retention_required, destruction_required, "
                        + "retention_ref_id, destruction_ref_id "
                        + "FROM inspection_task "
                        + "WHERE is_deleted = 0 AND current_step = 'DONE' AND task_status = '已完成' "
                        + "AND (retention_ref_id IS NULL "
                        + "OR (destruction_required = 1 AND destruction_ref_id IS NULL)) "
                        + "ORDER BY id");
    }

    /**
     * 逐条回退任务并同步六步状态
     *
     * @param tasks 待回退任务
     * @return 实际回退的任务数
     */
    private int reopen(List<Map<String, Object>> tasks) {
        int reopened = 0;
        for (Map<String, Object> task : tasks) {
            Long id = ((Number) task.get("id")).longValue();
            boolean retentionMissing = task.get("retention_ref_id") == null;
            int rows;
            if (retentionMissing) {
                rows = jdbcTemplate.update(
                        "UPDATE inspection_task SET current_step = 'RETENTION', task_status = '进行中', "
                                + "complete_time = NULL, updated_time = NOW(), version = version + 1 "
                                + "WHERE id = ? AND is_deleted = 0 AND current_step = 'DONE' "
                                + "AND task_status = '已完成'", id);
            } else {
                rows = jdbcTemplate.update(
                        "UPDATE inspection_task SET current_step = 'DESTRUCTION', task_status = '进行中', "
                                + "complete_time = NULL, updated_time = NOW(), version = version + 1 "
                                + "WHERE id = ? AND is_deleted = 0 AND current_step = 'DONE' "
                                + "AND task_status = '已完成'", id);
            }
            if (rows == 0) {
                log.warn("任务 {} 状态已变化，跳过回退", id);
                continue;
            }
            reopened++;
            if (retentionMissing) {
                syncStep(id, "RETENTION", "进行中");
                syncStep(id, "DESTRUCTION", "待办理");
            } else {
                syncStep(id, "DESTRUCTION", "进行中");
            }
        }
        return reopened;
    }

    /**
     * 同步单个环节状态
     *
     * @param taskId   任务ID
     * @param stepCode 环节编码
     * @param status   目标状态
     */
    private void syncStep(Long taskId, String stepCode, String status) {
        if ("进行中".equals(status)) {
            jdbcTemplate.update(
                    "UPDATE inspection_task_step SET step_status = ?, complete_time = NULL, skip_reason = NULL, "
                            + "start_time = COALESCE(start_time, NOW()), updated_time = NOW(), version = version + 1 "
                            + "WHERE task_id = ? AND step_code = ? AND is_deleted = 0",
                    status, taskId, stepCode);
        } else {
            jdbcTemplate.update(
                    "UPDATE inspection_task_step SET step_status = ?, complete_time = NULL, skip_reason = NULL, "
                            + "updated_time = NOW(), version = version + 1 "
                            + "WHERE task_id = ? AND step_code = ? AND is_deleted = 0",
                    status, taskId, stepCode);
        }
    }

    // endregion
}
