-- ===================================
-- 操作日志 字段补充
-- 1) sys_operation_log 增加 trace_id、change_count，用于关联数据变更日志
-- 说明：
--   1. 全部通过 information_schema 判断，幂等，可重复执行
--   2. 不指定 AFTER，新增列追加到表尾，避免线上大表因列位置调整触发表重建
-- 执行库：8.134.48.54/erp_db（测试）、8.148.27.237/erp_db1（生产）
-- ===================================

-- -----------------------------------
-- 1. 变更 TraceID
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'trace_id');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sys_operation_log` ADD COLUMN `trace_id` VARCHAR(64) DEFAULT NULL COMMENT ''变更TraceID''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 2. 数据变更记录数
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'change_count');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sys_operation_log` ADD COLUMN `change_count` INT NOT NULL DEFAULT 0 COMMENT ''数据变更记录数''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 3. 数据变更是否截断
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'change_truncated');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sys_operation_log` ADD COLUMN `change_truncated` TINYINT NOT NULL DEFAULT 0 COMMENT ''数据变更是否截断：0否/1是''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
