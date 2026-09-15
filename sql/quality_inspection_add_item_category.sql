-- 迁移脚本：质量检验4个表新增"被检品属性"字段
-- 需手动执行：mysql -h host -P port -u user -p dbname < quality_inspection_add_item_category.sql

ALTER TABLE `sampling_record` ADD COLUMN `item_category` VARCHAR(20) DEFAULT NULL COMMENT '被检品属性：生产原料/中间产品/成品' AFTER `object_name`;
ALTER TABLE `inspection_record` ADD COLUMN `item_category` VARCHAR(20) DEFAULT NULL COMMENT '被检品属性：生产原料/中间产品/成品' AFTER `object_name`;
ALTER TABLE `inspection_request` ADD COLUMN `item_category` VARCHAR(20) DEFAULT NULL COMMENT '被检品属性：生产原料/中间产品/成品' AFTER `preparation_name`;
ALTER TABLE `retained_sample` ADD COLUMN `item_category` VARCHAR(20) DEFAULT NULL COMMENT '被检品属性：生产原料/中间产品/成品' AFTER `material_name`;
