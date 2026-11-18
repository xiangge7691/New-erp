-- ===================================
-- 20261118 排产/绩效后端对齐原型
-- 1) 绩效记录增加修改前绩效时长
-- 2) 删除班次模块表（班次不再参与排产校验）
-- 3) 班组去掉所属车间、启停状态（班组仅作人员快捷分组）
-- 执行环境：测试库 192.168.5.100 / erp_db，生产库需评估后执行
-- ===================================

-- 1. 绩效记录：修改前绩效时长（首次修改时写入）
ALTER TABLE `performance_record`
  ADD COLUMN `original_performance_hours` DECIMAL(10,2) DEFAULT NULL COMMENT '修改前绩效时长（小时）' AFTER `performance_hours`;

-- 2. 班次模块下线
DROP TABLE IF EXISTS `production_shift_team`;
DROP TABLE IF EXISTS `production_shift`;

-- 3. 班组字段精简（接口与实体已不再使用）
ALTER TABLE `production_team`
  DROP INDEX `idx_team_status`,
  DROP COLUMN `workshop`,
  DROP COLUMN `status`;
