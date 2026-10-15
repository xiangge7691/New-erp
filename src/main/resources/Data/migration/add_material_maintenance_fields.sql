-- ===================================
-- 物料信息增加养护相关字段（养护记录模块前置改造）
-- 1) material 增加 maintenance_notes（养护注意事项）
-- 2) material 增加 maintenance_cycle_days（养护周期，天）
-- 3) material 增加 key_maintenance（是否重点养护物料：1是/0否）
-- 说明：幂等，可重复执行
-- ===================================

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'maintenance_notes');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `material` ADD COLUMN `maintenance_notes` VARCHAR(500) DEFAULT NULL COMMENT ''养护注意事项（养护登记时只读带出，供检查/养护参考）'' AFTER `storage_requirement`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'maintenance_cycle_days');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `material` ADD COLUMN `maintenance_cycle_days` INT DEFAULT NULL COMMENT ''养护周期（天，用于计算库存明细下次养护日期；为空表示不纳入周期提醒）'' AFTER `maintenance_notes`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = 'key_maintenance');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `material` ADD COLUMN `key_maintenance` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否重点养护物料：1是/0否'' AFTER `maintenance_cycle_days`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
