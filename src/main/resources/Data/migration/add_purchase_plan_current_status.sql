-- ===================================
-- 采购计划增加采购进度列（current_status / current_status_date）
-- 背景：采购计划审批通过后自动生成采购订单，但计划状态一直停留在"已审批"，
--       需要跟踪审批后的采购进度（待采购/运输中/验收中/已入库/已结束/已作废/已归档）
-- 说明：current_status 复用采购订单状态词表，由关联采购订单状态变化时回写；
--       全部幂等，可重复执行
-- ===================================

-- purchase_plan 增列（幂等）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_plan' AND COLUMN_NAME = 'current_status');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `purchase_plan` ADD COLUMN `current_status` VARCHAR(20) DEFAULT NULL COMMENT ''采购进度（派生自关联采购订单状态）'' AFTER `purchase_order_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_plan' AND COLUMN_NAME = 'current_status_date');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `purchase_plan` ADD COLUMN `current_status_date` DATETIME DEFAULT NULL COMMENT ''采购进度更新日期'' AFTER `current_status`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量回填：已审批且已关联采购订单的计划，按订单当前状态初始化采购进度（幂等）
UPDATE `purchase_plan` p
JOIN `purchase_orders` o ON o.`id` = p.`purchase_order_id` AND o.`is_deleted` = 0
SET p.`current_status` = o.`status`,
    p.`current_status_date` = NOW()
WHERE p.`is_deleted` = 0
  AND p.`status` = '已审批'
  AND p.`purchase_order_id` IS NOT NULL;
