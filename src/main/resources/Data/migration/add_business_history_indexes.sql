-- ===================================
-- 业务变更历史查询索引补充
-- 1) sys_operation_log 增加业务对象 + 时间索引，用于按业务补全操作信息
-- 2) sys_data_change_log 增加表 + 业务主键 + 时间索引，用于加载变更时间线
-- 3) preparation_formula 增加制剂索引，用于解析制剂下的处方行
-- 说明：全部通过 information_schema 判断，幂等，可重复执行
-- 执行库：8.134.48.54/erp_db（测试）、8.148.27.237/erp_db1（生产）
-- ===================================

-- -----------------------------------
-- 1. 操作日志业务索引
-- -----------------------------------
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND INDEX_NAME = 'idx_op_biz_time');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `sys_operation_log` ADD INDEX `idx_op_biz_time` (`business_type`, `business_id`, `created_time`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 2. 数据变更日志业务索引
-- -----------------------------------
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_data_change_log' AND INDEX_NAME = 'idx_change_table_biz_time');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `sys_data_change_log` ADD INDEX `idx_change_table_biz_time` (`table_name`, `business_id`, `created_time`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 3. 处方明细制剂索引（表不存在时跳过）
-- -----------------------------------
SET @table_exists := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preparation_formula');
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preparation_formula' AND INDEX_NAME = 'idx_formula_preparation');
SET @ddl := IF(@table_exists = 0 OR @idx_exists > 0,
    'SELECT 1',
    'ALTER TABLE `preparation_formula` ADD INDEX `idx_formula_preparation` (`preparation_id`)');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
