-- ===================================
-- 制剂信息增加"批件过期时间"字段
-- 1) preparation 增加 approval_expiry_date 列
-- 说明：幂等，可重复执行
-- ===================================

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preparation' AND COLUMN_NAME = 'approval_expiry_date');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `preparation` ADD COLUMN `approval_expiry_date` DATE DEFAULT NULL COMMENT ''批件过期时间（批件有效期截止日期，为空表示未添加）'' AFTER `record_info`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
