-- ===================================
-- 生产管理模块字段扩展
-- ===================================

-- 1. 生产计划表新增字段
ALTER TABLE `production_plan`
ADD COLUMN `plan_name` VARCHAR(200) DEFAULT '' COMMENT '计划名称' AFTER `plan_number`,
ADD COLUMN `plan_production_time` DATETIME DEFAULT NULL COMMENT '计划生产时间' AFTER `archive_time`,
ADD COLUMN `delivery_time` DATETIME DEFAULT NULL COMMENT '需交付时间' AFTER `plan_production_time`,
ADD COLUMN `purchase_order_time` DATETIME DEFAULT NULL COMMENT '采购计划下达时间' AFTER `delivery_time`,
ADD COLUMN `is_urgent` INT DEFAULT 0 COMMENT '是否加急（0-否，1-是）' AFTER `purchase_order_time`,
ADD COLUMN `plan_file` VARCHAR(500) DEFAULT '' COMMENT '计划单文件' AFTER `is_urgent`;

-- 2. 工单表新增字段
ALTER TABLE `work_order`
ADD COLUMN `plan_id` BIGINT DEFAULT NULL COMMENT '关联计划ID' AFTER `preparation_name`,
ADD COLUMN `plan_name` VARCHAR(200) DEFAULT '' COMMENT '关联计划名称' AFTER `plan_id`,
ADD COLUMN `config_date` DATETIME DEFAULT NULL COMMENT '配置日期' AFTER `return_amount`,
ADD COLUMN `production_type` VARCHAR(50) DEFAULT '' COMMENT '加工类型（自主加工/委托加工/试生产）' AFTER `config_date`,
ADD COLUMN `sales_price` DECIMAL(10,2) DEFAULT NULL COMMENT '销售单价' AFTER `production_type`,
ADD COLUMN `finished_qty` DECIMAL(10,3) DEFAULT NULL COMMENT '成品数量' AFTER `sales_price`,
ADD COLUMN `production_cycle` INT DEFAULT NULL COMMENT '生产周期（天）' AFTER `finished_qty`,
ADD COLUMN `yield_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '得率（百分比）' AFTER `production_cycle`,
ADD COLUMN `total_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '总金额' AFTER `yield_rate`,
ADD COLUMN `current_status` VARCHAR(20) DEFAULT '待生产' COMMENT '当前状态' AFTER `total_amount`,
ADD COLUMN `production_complete_time` DATETIME DEFAULT NULL COMMENT '生产完成时间' AFTER `current_status`,
ADD COLUMN `inspection_start` DATETIME DEFAULT NULL COMMENT '检验开始时间' AFTER `production_complete_time`,
ADD COLUMN `inspection_end` DATETIME DEFAULT NULL COMMENT '检验结束时间' AFTER `inspection_start`,
ADD COLUMN `outbound_time` DATETIME DEFAULT NULL COMMENT '出库时间' AFTER `inspection_end`,
ADD COLUMN `archive_time` DATETIME DEFAULT NULL COMMENT '归档时间' AFTER `outbound_time`;

-- 3. 制剂表新增字段
ALTER TABLE `preparation`
ADD COLUMN `retail_price` DECIMAL(10,2) DEFAULT NULL COMMENT '零售单价' AFTER `settlement_price`;
