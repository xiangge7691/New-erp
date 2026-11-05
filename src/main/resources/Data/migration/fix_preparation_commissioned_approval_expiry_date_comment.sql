-- ===================================
-- 修复制剂信息“委托配制批件期限”字段备注显示为问号的问题
-- 背景：历史执行 add_preparation_commissioned_approval_expiry_date.sql 时，
--       数据库连接字符集不支持中文，导致列备注被写成“?”。
-- 处理：重新写入正确的列备注；幂等，可重复执行。
-- 注意：执行前请确保客户端连接字符集为 utf8mb4（脚本已内置 SET NAMES）。
-- ===================================

SET NAMES utf8mb4;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preparation' AND COLUMN_NAME = 'commissioned_approval_expiry_date');

SET @ddl := IF(@col_exists = 1,
    'ALTER TABLE `preparation` MODIFY COLUMN `commissioned_approval_expiry_date` DATE DEFAULT NULL COMMENT ''委托配制批件期限（委托配制批件有效期截止日期，为空表示未添加）''',
    'SELECT 1');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
