-- ===================================
-- 制剂信息增加"委托配制批件期限"字段
-- 1) preparation 增加 commissioned_approval_expiry_date 列
-- 说明：幂等，可重复执行
-- 注意：DDL 中含中文备注，必须先设置连接字符集，否则客户端字符集不支持中文时
--       备注会被写成“?”（若已出现问号备注，执行 fix_preparation_commissioned_approval_expiry_date_comment.sql 修复）
-- ===================================

SET NAMES utf8mb4;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preparation' AND COLUMN_NAME = 'commissioned_approval_expiry_date');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `preparation` ADD COLUMN `commissioned_approval_expiry_date` DATE DEFAULT NULL COMMENT ''委托配制批件期限（委托配制批件有效期截止日期，为空表示未添加）'' AFTER `approval_expiry_date`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
