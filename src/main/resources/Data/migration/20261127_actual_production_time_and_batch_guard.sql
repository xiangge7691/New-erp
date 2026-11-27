-- ===================================
-- 实际生产：批次唯一约束、历史状态回写与时间巡检
-- 文件：20261127_actual_production_time_and_batch_guard.sql
-- 说明：
--   1) 同一排产批次只允许一条执行记录，防止保存批次 B 时覆盖批次 A；
--   2) 回写历史执行记录 status，使其与起止时间一致（读路径已按时间推导，回写只是兜底）；
--   3) 附带只读巡检 SQL，执行前先确认无重复、无倒挂。
-- 执行前请先备份 work_order_process_execution。
-- ===================================

-- ===================================
-- 一、执行前巡检（只读）
-- ===================================

-- 1.1 同一批次存在多条执行记录（有结果时先人工合并，不能直接建唯一索引）
SELECT plan_batch_id,
       COUNT(*)                          AS cnt,
       GROUP_CONCAT(id ORDER BY id)      AS execution_ids
FROM work_order_process_execution
WHERE plan_batch_id IS NOT NULL
  AND is_deleted = 0
GROUP BY plan_batch_id
HAVING COUNT(*) > 1;

-- 1.2 工序时间倒挂：后道工序已开始，但前道工序的实际结束晚于它的开始
SELECT e.id            AS execution_id,
       e.work_order_id,
       e.step_order    AS step_order,
       e.start_time,
       e.end_time,
       p.step_order    AS prev_step_order,
       p.end_time      AS prev_end_time
FROM work_order_process_execution e
JOIN work_order_process_execution p
  ON p.work_order_id = e.work_order_id
 AND p.step_order < e.step_order
 AND p.is_deleted = 0
WHERE e.is_deleted = 0
  AND e.start_time IS NOT NULL
  AND p.end_time IS NOT NULL
  AND p.end_time > e.start_time
ORDER BY e.work_order_id, e.step_order, e.id;

-- 1.3 实际时间超出工单配置时间窗（配置时间为空的行跳过）
SELECT e.id AS execution_id, e.work_order_id, e.step_order, e.start_time, e.end_time,
       w.config_date, w.config_complete_time
FROM work_order_process_execution e
JOIN work_order w ON w.work_order_id = e.work_order_id AND w.is_deleted = 0
WHERE e.is_deleted = 0
  AND ((w.config_date IS NOT NULL AND e.start_time IS NOT NULL AND e.start_time < w.config_date)
    OR (w.config_complete_time IS NOT NULL AND e.end_time IS NOT NULL AND e.end_time > w.config_complete_time))
ORDER BY e.work_order_id, e.step_order;

-- 1.4 status 与起止时间不一致
SELECT id, work_order_id, step_order, start_time, end_time, status
FROM work_order_process_execution
WHERE is_deleted = 0
  AND (status IS NULL OR status <> CASE
        WHEN start_time IS NULL THEN '待执行'
        WHEN end_time IS NULL THEN '执行中'
        ELSE '已完成'
      END);

-- ===================================
-- 二、历史状态回写（与 ProcessExecutionStatus.resolve 口径一致）
-- ===================================
UPDATE work_order_process_execution
SET status = CASE
        WHEN start_time IS NULL THEN '待执行'
        WHEN end_time IS NULL THEN '执行中'
        ELSE '已完成'
    END
WHERE is_deleted = 0
  AND (status IS NULL OR status <> CASE
        WHEN start_time IS NULL THEN '待执行'
        WHEN end_time IS NULL THEN '执行中'
        ELSE '已完成'
      END);

-- ===================================
-- 三、同一批次唯一索引（仅当无重复且索引不存在时创建）
--    MySQL 唯一索引允许多个 NULL，未拆批次的工序级执行记录不受影响；
--    唯一索引对软删除行同样生效，因此重复检查包含 is_deleted = 1 的历史行
-- ===================================
SET @dup_cnt := (
    SELECT COUNT(*) FROM (
        SELECT plan_batch_id
        FROM work_order_process_execution
        WHERE plan_batch_id IS NOT NULL
        GROUP BY plan_batch_id
        HAVING COUNT(*) > 1
    ) t
);
SET @has_uk := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order_process_execution'
      AND index_name = 'uk_wpe_plan_batch'
);
SET @sql := IF(@dup_cnt = 0 AND @has_uk = 0,
    'ALTER TABLE work_order_process_execution ADD UNIQUE KEY uk_wpe_plan_batch (plan_batch_id)',
    'SELECT ''跳过 uk_wpe_plan_batch：存在重复批次或索引已存在'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===================================
-- 四、按工单+工序查询索引（仅当索引不存在时创建）
-- ===================================
SET @has_step_idx := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order_process_execution'
      AND index_name = 'idx_wpe_work_order_step'
);
SET @sql := IF(@has_step_idx = 0,
    'ALTER TABLE work_order_process_execution ADD KEY idx_wpe_work_order_step (work_order_id, step_order)',
    'SELECT ''跳过 idx_wpe_work_order_step：索引已存在'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
