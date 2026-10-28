-- ===================================
-- 制剂信息增加"预计成本价"字段
-- 1) preparation 增加 estimated_cost_price 列（元/单位）
-- 2) 用于统计图「预估成本」：预估成本 = 预计成本价 × 工单批量
-- 说明：幂等，可重复执行；存量数据为 NULL，统计时按 0 计算
-- ===================================

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preparation' AND COLUMN_NAME = 'estimated_cost_price');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `preparation` ADD COLUMN `estimated_cost_price` DECIMAL(18,2) DEFAULT NULL COMMENT ''预计成本价（元/单位，用于预估成本统计）'' AFTER `sales_price`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
