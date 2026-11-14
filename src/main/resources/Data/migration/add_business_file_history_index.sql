-- ===================================
-- 业务附件变更历史查询索引
-- 业务变更历史会按 root_type + operation_type + created_time 过滤 file_operation_log，
-- 再按 detail 中的 bizType/bizId 匹配业务对象；先缩小时间范围可避免全表扫描。
-- 说明：通过 information_schema 判断索引是否存在，幂等，可重复执行
-- 执行库：8.134.48.54/erp_db（测试）、8.148.27.237/erp_db1（生产）
-- ===================================

SET @table_exists := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'file_operation_log');
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'file_operation_log'
      AND INDEX_NAME = 'idx_file_op_type_time');
SET @ddl := IF(@table_exists = 0 OR @idx_exists > 0,
    'SELECT 1',
    'ALTER TABLE `file_operation_log` ADD INDEX `idx_file_op_type_time` (`root_type`, `operation_type`, `created_time`)');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
