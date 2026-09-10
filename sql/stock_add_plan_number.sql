-- 迁移脚本：库存表新增"关联生产计划编号"字段（入库时自动写入）
-- 需手动执行：mysql -h 8.134.48.54 -P 3306 -u erp_db -p erp_db < stock_add_plan_number.sql

ALTER TABLE `stock` ADD COLUMN `plan_number` VARCHAR(50) DEFAULT NULL COMMENT '关联生产计划编号（入库时自动写入）' AFTER `storage_location`;
