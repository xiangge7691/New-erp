-- ===================================
-- 制剂信息增加“续展备注”字段
-- 1) preparation 增加 renewal_remark 列
-- 说明：幂等，可重复执行
-- 注意：DDL 中含中文备注，必须先设置连接字符集，否则客户端字符集不支持中文时
--       备注会被写成“?”
-- ===================================

SET NAMES utf8mb4;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preparation' AND COLUMN_NAME = 'renewal_remark');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `preparation` ADD COLUMN `renewal_remark` VARCHAR(1000) DEFAULT NULL COMMENT ''续展备注（续展申报说明，可为空）'' AFTER `commissioned_approval_expiry_date`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
