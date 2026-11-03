-- ===================================
-- 采购流程节点时间字段（不新增表）
-- 背景：生产计划需要展示关联采购的流程进度：
--       采购申请 -> 审批 -> 下单 -> 到货 -> 验收 -> 入库
-- 说明：采购申请时间复用 purchase_plan.created_time，入库时间复用 stock_in.in_date，
--       本脚本只补充审批、下单、到货、验收完成四个节点时间。
--       全部幂等，可重复执行。
-- ===================================

-- purchase_plan：审批完成/驳回时间
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_plan' AND COLUMN_NAME = 'approval_time');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `purchase_plan` ADD COLUMN `approval_time` DATETIME DEFAULT NULL COMMENT ''审批完成/驳回时间'' AFTER `status`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- purchase_orders：确认采购/下单时间
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_orders' AND COLUMN_NAME = 'order_time');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `purchase_orders` ADD COLUMN `order_time` DATETIME DEFAULT NULL COMMENT ''确认采购/下单时间'' AFTER `status`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- acceptance_order：确认到货时间
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_order' AND COLUMN_NAME = 'arrival_time');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_order` ADD COLUMN `arrival_time` DATETIME DEFAULT NULL COMMENT ''确认到货时间'' AFTER `status`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- acceptance_order：验收完成时间（全部验收明细终结）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_order' AND COLUMN_NAME = 'acceptance_time');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_order` ADD COLUMN `acceptance_time` DATETIME DEFAULT NULL COMMENT ''验收完成时间（全部明细终结）'' AFTER `arrival_time`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
