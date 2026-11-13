-- ===================================
-- 日志模块 字段补充：客户端 MAC 地址
-- 1) sys_login_log 增加 mac_address
-- 2) sys_operation_log 增加 mac_address
-- 说明：
--   1. 全部通过 information_schema 判断，幂等，可重复执行
--   2. 不指定 AFTER，新增列追加到表尾，避免线上大表因列位置调整触发表重建
--   3. MAC 由客户端通过请求头 X-Client-Mac 上报，服务端统一格式化为 AA-BB-CC-DD-EE-FF
--   4. 存量日志的 mac_address 为空，新记录开始写入
-- 执行库：8.134.48.54/erp_db（测试）、8.148.27.237/erp_db1（生产）
-- ===================================

-- -----------------------------------
-- 1. 登录日志 MAC 地址
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_login_log' AND COLUMN_NAME = 'mac_address');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sys_login_log` ADD COLUMN `mac_address` VARCHAR(32) DEFAULT NULL COMMENT ''客户端MAC地址（客户端通过 X-Client-Mac 上报，格式 AA-BB-CC-DD-EE-FF）''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_login_log' AND INDEX_NAME = 'idx_login_mac');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `sys_login_log` ADD INDEX `idx_login_mac` (`mac_address`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 2. 操作日志 MAC 地址
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND COLUMN_NAME = 'mac_address');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sys_operation_log` ADD COLUMN `mac_address` VARCHAR(32) DEFAULT NULL COMMENT ''客户端MAC地址（客户端通过 X-Client-Mac 上报，格式 AA-BB-CC-DD-EE-FF）''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_operation_log' AND INDEX_NAME = 'idx_op_mac_time');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `sys_operation_log` ADD INDEX `idx_op_mac_time` (`mac_address`, `created_time`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
