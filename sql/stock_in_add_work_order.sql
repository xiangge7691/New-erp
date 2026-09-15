-- 迁移脚本：入库单主表新增"关联生产任务"字段
-- 需手动执行：mysql -h host -P port -u user -p dbname < stock_in_add_work_order.sql

ALTER TABLE `stock_in` ADD COLUMN `work_order_id` BIGINT DEFAULT NULL COMMENT '关联生产任务ID（成品入库时必填）' AFTER `plan_number`;
ALTER TABLE `stock_in` ADD COLUMN `work_order_code` VARCHAR(50) DEFAULT NULL COMMENT '关联生产任务编号（成品入库时必填）' AFTER `work_order_id`;
