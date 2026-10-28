-- ===================================
-- 删除房间表中误加的清洁方式字段
-- 说明：
--   1. 清洁方式/清洁类型实际应属于清洁记录（cleaning_record.cleaning_type）
--   2. 通过 information_schema 判断，幂等，可重复执行
-- 执行库：8.134.48.54/erp_db（测试）、8.148.27.237/erp_db1（生产）
-- ===================================

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'room_info' AND COLUMN_NAME = 'cleaning_method');
SET @ddl := IF(@col_exists > 0,
    'ALTER TABLE `room_info` DROP COLUMN `cleaning_method`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
