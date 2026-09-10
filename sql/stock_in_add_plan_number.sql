-- 迁移脚本：入库单主表新增"关联生产计划编号"字段（人工填写）
-- 需手动执行：mysql -h 8.134.48.54 -P 3306 -u erp_db -p erp_db < stock_in_add_plan_number.sql

ALTER TABLE `stock_in` ADD COLUMN `plan_number` VARCHAR(50) DEFAULT NULL COMMENT '关联生产计划编号（人工填写）' AFTER `related_order`;