-- ===================================
-- 采购流程进度查询索引（幂等）
-- 背景：生产计划列表/详情按 production_plan_id、plan_id、purchase_number、
--       acceptance_code、plan_number 批量查询采购链路，补齐关联字段索引。
-- 说明：MySQL 不支持 CREATE INDEX IF NOT EXISTS，使用 information_schema 判断。
-- ===================================

-- purchase_orders.production_plan_id
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_orders' AND INDEX_NAME = 'idx_po_production_plan');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `purchase_orders` ADD INDEX `idx_po_production_plan` (`production_plan_id`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- purchase_orders.plan_id
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_orders' AND INDEX_NAME = 'idx_po_plan');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `purchase_orders` ADD INDEX `idx_po_plan` (`plan_id`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- acceptance_order.purchase_number
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_order' AND INDEX_NAME = 'idx_acceptance_purchase_number');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `acceptance_order` ADD INDEX `idx_acceptance_purchase_number` (`purchase_number`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- acceptance_order.plan_code
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_order' AND INDEX_NAME = 'idx_acceptance_plan_code');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `acceptance_order` ADD INDEX `idx_acceptance_plan_code` (`plan_code`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- stock_in.related_order
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_in' AND INDEX_NAME = 'idx_stock_in_related_order');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `stock_in` ADD INDEX `idx_stock_in_related_order` (`related_order`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- stock_in.plan_number
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_in' AND INDEX_NAME = 'idx_stock_in_plan_number');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `stock_in` ADD INDEX `idx_stock_in_plan_number` (`plan_number`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
