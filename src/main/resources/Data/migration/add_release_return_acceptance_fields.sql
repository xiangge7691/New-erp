-- ===================================
-- 审核放行 / 退库管理 / 货物验收 字段补充
-- 1) release_review 增加被检品属性
-- 2) return_order 增加外包装检查、性状检查、退库人、验收人、审核人、退库时间
-- 3) acceptance_order 增加外包装检查、性状检查、验收人、审核人
-- 4) acceptance_detail 增加外包装检查、性状检查
-- 说明：
--   1. 全部通过 information_schema 判断，幂等，可重复执行
--   2. 不指定 AFTER，新增列追加到表尾，避免线上大表因列位置调整触发表重建
--   3. 检查结果列默认“合格”，存量记录会按默认值回填
-- 执行库：8.134.48.54/erp_db（测试）、8.148.27.237/erp_db1（生产）
-- ===================================

-- -----------------------------------
-- 1. release_review 增加被检品属性
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'release_review' AND COLUMN_NAME = 'item_category');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `release_review` ADD COLUMN `item_category` VARCHAR(20) DEFAULT NULL COMMENT ''被检品属性：生产原料/中间产品/成品''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 2. return_order 增加检查、人员与退库时间字段
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'return_order' AND COLUMN_NAME = 'outer_package_check');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `return_order` ADD COLUMN `outer_package_check` VARCHAR(10) NOT NULL DEFAULT ''合格'' COMMENT ''外包装检查：合格/不合格''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'return_order' AND COLUMN_NAME = 'appearance_check');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `return_order` ADD COLUMN `appearance_check` VARCHAR(10) DEFAULT ''合格'' COMMENT ''性状检查：合格/不合格''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'return_order' AND COLUMN_NAME = 'return_person_name');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `return_order` ADD COLUMN `return_person_name` VARCHAR(50) DEFAULT NULL COMMENT ''退库人''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'return_order' AND COLUMN_NAME = 'acceptor_name');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `return_order` ADD COLUMN `acceptor_name` VARCHAR(50) DEFAULT NULL COMMENT ''验收人''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'return_order' AND COLUMN_NAME = 'reviewer_name');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `return_order` ADD COLUMN `reviewer_name` VARCHAR(50) DEFAULT NULL COMMENT ''审核人''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'return_order' AND COLUMN_NAME = 'return_time');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `return_order` ADD COLUMN `return_time` DATETIME DEFAULT NULL COMMENT ''退库时间''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 3. acceptance_order 增加检查与人员字段
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_order' AND COLUMN_NAME = 'outer_package_check');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_order` ADD COLUMN `outer_package_check` VARCHAR(10) DEFAULT ''合格'' COMMENT ''外包装检查：合格/不合格''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_order' AND COLUMN_NAME = 'appearance_check');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_order` ADD COLUMN `appearance_check` VARCHAR(10) DEFAULT ''合格'' COMMENT ''性状检查：合格/不合格''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_order' AND COLUMN_NAME = 'acceptor_name');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_order` ADD COLUMN `acceptor_name` VARCHAR(50) DEFAULT NULL COMMENT ''验收人''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_order' AND COLUMN_NAME = 'reviewer_name');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_order` ADD COLUMN `reviewer_name` VARCHAR(50) DEFAULT NULL COMMENT ''审核人''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 4. acceptance_detail 增加检查字段
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_detail' AND COLUMN_NAME = 'outer_package_check');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_detail` ADD COLUMN `outer_package_check` VARCHAR(10) DEFAULT ''合格'' COMMENT ''外包装检查：合格/不合格''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_detail' AND COLUMN_NAME = 'appearance_check');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_detail` ADD COLUMN `appearance_check` VARCHAR(10) DEFAULT ''合格'' COMMENT ''性状检查：合格/不合格''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 5. 执行结果校验（应返回 13 行）
-- -----------------------------------
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (
    (TABLE_NAME = 'release_review' AND COLUMN_NAME = 'item_category')
    OR (TABLE_NAME = 'return_order' AND COLUMN_NAME IN
        ('outer_package_check', 'appearance_check', 'return_person_name', 'acceptor_name', 'reviewer_name', 'return_time'))
    OR (TABLE_NAME = 'acceptance_order' AND COLUMN_NAME IN
        ('outer_package_check', 'appearance_check', 'acceptor_name', 'reviewer_name'))
    OR (TABLE_NAME = 'acceptance_detail' AND COLUMN_NAME IN
        ('outer_package_check', 'appearance_check'))
  )
ORDER BY TABLE_NAME, COLUMN_NAME;
