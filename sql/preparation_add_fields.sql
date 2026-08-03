-- 为 preparation 表添加销售单价字段
-- 创建时间：2026-08-03

ALTER TABLE `preparation`
ADD COLUMN `sales_price` DECIMAL(10,2) NULL DEFAULT NULL COMMENT '销售单价' AFTER `retail_price`;
