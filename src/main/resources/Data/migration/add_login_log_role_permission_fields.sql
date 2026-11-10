-- ===================================
-- 登录日志 字段补充
-- 1) sys_login_log 增加角色、菜单范围、接口权限快照
-- 说明：
--   1. 全部通过 information_schema 判断，幂等，可重复执行
--   2. 不指定 AFTER，新增列追加到表尾，避免线上大表因列位置调整触发表重建
--   3. 存量登录日志的角色/权限字段为空，新登录记录开始写入快照
-- 执行库：8.134.48.54/erp_db（测试）、8.148.27.237/erp_db1（生产）
-- ===================================

-- -----------------------------------
-- 1. 角色快照
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_login_log' AND COLUMN_NAME = 'role_ids');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sys_login_log` ADD COLUMN `role_ids` VARCHAR(500) DEFAULT NULL COMMENT ''角色ID快照，逗号分隔''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_login_log' AND COLUMN_NAME = 'role_names');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sys_login_log` ADD COLUMN `role_names` VARCHAR(500) DEFAULT NULL COMMENT ''角色名称快照，顿号分隔''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 2. 菜单范围快照
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_login_log' AND COLUMN_NAME = 'menu_count');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sys_login_log` ADD COLUMN `menu_count` INT NOT NULL DEFAULT 0 COMMENT ''可访问菜单数''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_login_log' AND COLUMN_NAME = 'menu_keys');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sys_login_log` ADD COLUMN `menu_keys` TEXT DEFAULT NULL COMMENT ''菜单权限键快照，逗号分隔''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_login_log' AND COLUMN_NAME = 'menu_names');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sys_login_log` ADD COLUMN `menu_names` TEXT DEFAULT NULL COMMENT ''菜单名称快照，顿号分隔''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 3. 接口权限快照
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_login_log' AND COLUMN_NAME = 'permission_count');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sys_login_log` ADD COLUMN `permission_count` INT NOT NULL DEFAULT 0 COMMENT ''接口权限数''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_login_log' AND COLUMN_NAME = 'permission_keys');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sys_login_log` ADD COLUMN `permission_keys` TEXT DEFAULT NULL COMMENT ''接口权限键快照，逗号分隔''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_login_log' AND COLUMN_NAME = 'permission_names');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sys_login_log` ADD COLUMN `permission_names` TEXT DEFAULT NULL COMMENT ''接口权限名称快照，顿号分隔''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
