-- 生产计划表新增制剂ID字段
ALTER TABLE `production_plan`
ADD COLUMN `preparation_id` BIGINT DEFAULT NULL COMMENT '制剂ID' AFTER `related_order`;
