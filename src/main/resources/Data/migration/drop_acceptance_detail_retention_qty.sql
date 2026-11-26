-- ===================================
-- 货物验收·移除留样数量字段
-- 1) acceptance_detail 删除已废弃的留样数量列（retention_qty）
-- 说明：
--   1. 通过 information_schema 判断列是否存在，幂等，可重复执行
--   2. 验收环节只记录取样数量，留样数量不再落库
-- 执行库：192.168.5.100/erp_db（测试）
-- ===================================

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_detail' AND COLUMN_NAME = 'retention_qty');
SET @ddl := IF(@col_exists > 0,
    'ALTER TABLE `acceptance_detail` DROP COLUMN `retention_qty`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
