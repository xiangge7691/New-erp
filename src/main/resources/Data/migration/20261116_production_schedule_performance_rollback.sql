-- ===================================
-- 生产排产 · 实际执行 · 人员绩效 回滚脚本
-- 目标库：192.168.5.100 / erp_db
-- 说明：
--   1) 删除本次新增的表与字段
--   2) 数据回填（plan_start_time、状态、operator_id、参与人）无法自动还原，
--      如需完整回滚，请使用执行前备份：
--      _analysis/db-backup/erp_db_20261116_before_scheduling.sql
-- ===================================

DROP TABLE IF EXISTS `performance_record`;
DROP TABLE IF EXISTS `performance_person_coefficient`;
DROP TABLE IF EXISTS `performance_process_coefficient`;
DROP TABLE IF EXISTS `performance_coefficient_version`;
DROP TABLE IF EXISTS `work_order_process_execution_participant`;
DROP TABLE IF EXISTS `work_order_process_plan_participant`;
DROP TABLE IF EXISTS `work_order_process_plan_batch`;
DROP TABLE IF EXISTS `work_order_process_plan`;
DROP TABLE IF EXISTS `production_shift_team`;
DROP TABLE IF EXISTS `production_shift`;
DROP TABLE IF EXISTS `production_team_member`;
DROP TABLE IF EXISTS `production_team`;

ALTER TABLE `work_order_process_execution`
  DROP INDEX `idx_wpe_plan`,
  DROP INDEX `idx_wpe_plan_batch`,
  DROP COLUMN `completed_time`,
  DROP COLUMN `locked`,
  DROP COLUMN `team_id`,
  DROP COLUMN `batch_no`,
  DROP COLUMN `plan_batch_id`,
  DROP COLUMN `plan_id`;

ALTER TABLE `process_type`
  DROP COLUMN `coefficient`;

ALTER TABLE `work_order`
  DROP INDEX `idx_work_order_plan_start`,
  DROP COLUMN `plan_start_time`;
