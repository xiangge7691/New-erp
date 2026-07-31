-- 为 work_order 表添加配置完成日期和备注字段
-- 创建时间：2025-07-31

-- 添加配置完成日期字段
ALTER TABLE `work_order` 
ADD COLUMN `config_complete_time` DATETIME NULL DEFAULT NULL COMMENT '配置完成日期' AFTER `config_date`;

-- 添加备注字段  
ALTER TABLE `work_order`
ADD COLUMN `remark` VARCHAR(500) NULL DEFAULT '' COMMENT '备注' AFTER `config_complete_time`;

