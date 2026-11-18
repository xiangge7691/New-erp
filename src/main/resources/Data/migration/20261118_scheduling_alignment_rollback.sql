-- ===================================
-- 20261118 排产/绩效后端对齐原型 - 回滚
-- 仅回滚可行部分：恢复班组字段、删除绩效原值列
-- 班次表已删除，如需恢复请从 20261116 迁移脚本重新建表
-- ===================================

ALTER TABLE `production_team`
  ADD COLUMN `workshop` VARCHAR(100) DEFAULT NULL COMMENT '所属车间' AFTER `team_name`,
  ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用/1启用' AFTER `workshop`,
  ADD KEY `idx_team_status` (`status`);

ALTER TABLE `performance_record`
  DROP COLUMN `original_performance_hours`;
