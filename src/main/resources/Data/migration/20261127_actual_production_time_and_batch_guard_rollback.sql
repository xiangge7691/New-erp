-- ===================================
-- 实际生产批次约束回滚
-- 文件：20261127_actual_production_time_and_batch_guard_rollback.sql
-- 说明：仅回滚索引；status 回写属于数据修正，回滚请使用执行前的备份。
-- ===================================

SET @has_uk := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order_process_execution'
      AND index_name = 'uk_wpe_plan_batch'
);
SET @sql := IF(@has_uk > 0,
    'ALTER TABLE work_order_process_execution DROP INDEX uk_wpe_plan_batch',
    'SELECT ''uk_wpe_plan_batch 不存在，跳过'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_step_idx := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'work_order_process_execution'
      AND index_name = 'idx_wpe_work_order_step'
);
SET @sql := IF(@has_step_idx > 0,
    'ALTER TABLE work_order_process_execution DROP INDEX idx_wpe_work_order_step',
    'SELECT ''idx_wpe_work_order_step 不存在，跳过'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
