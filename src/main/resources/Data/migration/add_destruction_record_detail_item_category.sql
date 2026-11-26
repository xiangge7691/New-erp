-- ===================================
-- 销毁记录·明细品名类别与计量单位
-- 1) destruction_record_detail 增加品名类别（item_category）、计量单位（unit_name）
-- 说明：
--   1. 全部通过 information_schema 判断，幂等，可重复执行
--   2. 不指定 AFTER，新增列追加到表尾，避免大表因列位置调整触发表重建
--   3. 品名类别取值：生产原料/中间产品/成品，仅「不合格品销毁」类型保存时必填
-- 执行库：192.168.5.100/erp_db（测试）
-- ===================================

-- -----------------------------------
-- 1. destruction_record_detail 增加品名类别
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'destruction_record_detail' AND COLUMN_NAME = 'item_category');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `destruction_record_detail` ADD COLUMN `item_category` VARCHAR(20) DEFAULT NULL COMMENT ''品名类别：生产原料/中间产品/成品（不合格品销毁类型必填）''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 2. destruction_record_detail 增加计量单位
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'destruction_record_detail' AND COLUMN_NAME = 'unit_name');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `destruction_record_detail` ADD COLUMN `unit_name` VARCHAR(20) DEFAULT NULL COMMENT ''计量单位（不合格品销毁类型必填）''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
