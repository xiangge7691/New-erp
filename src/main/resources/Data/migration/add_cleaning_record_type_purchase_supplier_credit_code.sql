-- ===================================
-- 清洁记录 / 供应商信息 字段补充
-- 1) cleaning_record 增加清洁类型
-- 2) purchase_suppliers 增加统一社会信用代码
-- 说明：
--   1. 全部通过 information_schema 判断，幂等，可重复执行
--   2. 不指定 AFTER，新增列追加到表尾，避免线上大表因列位置调整触发表重建
--   3. 清洁类型为可空字段，存量记录不回填，新增/修改时由后端校验取值
-- 执行库：8.134.48.54/erp_db（测试）、8.148.27.237/erp_db1（生产）
-- ===================================

-- -----------------------------------
-- 1. cleaning_record 增加清洁类型
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cleaning_record' AND COLUMN_NAME = 'cleaning_type');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `cleaning_record` ADD COLUMN `cleaning_type` VARCHAR(50) DEFAULT NULL COMMENT ''清洁类型：小清场/大清场/公共区定期清洁消毒''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 2. purchase_suppliers 增加统一社会信用代码
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_suppliers' AND COLUMN_NAME = 'social_credit_code');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `purchase_suppliers` ADD COLUMN `social_credit_code` VARCHAR(50) DEFAULT NULL COMMENT ''统一社会信用代码''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
