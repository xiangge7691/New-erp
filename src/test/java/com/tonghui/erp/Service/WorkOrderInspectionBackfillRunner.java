package com.tonghui.erp.Service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 存量工单检验结束时间补数（一次性运维工具，不在常规测试中执行）
 * <p>
 * 使用方式：./mvnw test -Dtest=WorkOrderInspectionBackfillRunner
 * 逻辑：找出已存在“成品 + 合格”检验记录、但工单 inspection_end 仍为空且状态应为已检验的工单，
 * 逐条调用 syncWorkOrderTime 回填结束时间，同时重算工单状态并刷新生产计划。
 * syncWorkOrderTime 只在字段为空时写入，可重复执行。
 * </p>
 */
@SpringBootTest
public class WorkOrderInspectionBackfillRunner {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** 工单服务，复用正式的状态同步与计划刷新逻辑 */
    @Autowired
    private WorkOrderService workOrderService;

    /** JDBC模板，用于查询待修复工单与检验记录 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // endregion

    // region 补数
    // ===================================
    // 补数
    // ===================================

    /**
     * 回填存量工单的检验结束时间
     */
    @Test
    public void backfillInspectionEnd() {
        List<Object[]> rows = jdbcTemplate.query(
                "SELECT w.work_order_id, r.end_time FROM work_order w "
                        + "JOIN ("
                        + "  SELECT work_order_id, MAX(end_time) AS end_time FROM inspection_record "
                        + "  WHERE is_deleted = 0 AND item_category = '成品' AND conclusion = '合格' "
                        + "    AND end_time IS NOT NULL AND work_order_id IS NOT NULL "
                        + "  GROUP BY work_order_id"
                        + ") r ON r.work_order_id = w.work_order_id "
                        + "WHERE w.is_deleted = 0 AND w.inspection_end IS NULL "
                        + "  AND (w.current_status IS NULL OR w.current_status IN ('已生产', '检验中')) "
                        + "ORDER BY w.work_order_id",
                (rs, rowNum) -> new Object[]{rs.getLong(1), rs.getTimestamp(2).toLocalDateTime()});

        int updated = 0;
        int failed = 0;
        for (Object[] row : rows) {
            Long workOrderId = (Long) row[0];
            LocalDateTime endTime = (LocalDateTime) row[1];
            try {
                if (workOrderService.syncWorkOrderTime(workOrderId, "inspectionEnd", endTime)) {
                    updated++;
                }
            } catch (Exception e) {
                failed++;
                System.out.println("[Backfill] 工单 " + workOrderId + " 回填失败：" + e.getMessage());
            }
        }
        System.out.println("[Backfill] 待修复工单 " + rows.size() + " 条，成功回填 " + updated
                + " 条，失败 " + failed + " 条");
    }

    // endregion
}
