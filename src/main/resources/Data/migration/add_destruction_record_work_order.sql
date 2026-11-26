-- ===================================
-- 销毁记录·生产任务关联
-- 1) destruction_record 增加关联生产任务字段（work_order_id / work_order_code / work_order_name）
-- 说明：
--   1. 全部通过 information_schema 判断，幂等，可重复执行
--   2. 不指定 AFTER，新增列追加到表尾，避免大表因列位置调整触发表重建
--   3. 关联生产任务为非必填，仅作追溯引用；不联动生产任务状态，也不关联库存
--   4. work_order_code / work_order_name 为保存时的快照，避免任务改名后台账历史被篡改
-- 执行库：192.168.5.100/erp_db（测试）
-- ===================================

-- -----------------------------------
-- 1. 关联生产任务ID
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'destruction_record' AND COLUMN_NAME = 'work_order_id');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `destruction_record` ADD COLUMN `work_order_id` BIGINT DEFAULT NULL COMMENT ''关联生产任务ID（可空，仅追溯引用，不联动任务状态）''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 2. 生产任务编号快照
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'destruction_record' AND COLUMN_NAME = 'work_order_code');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `destruction_record` ADD COLUMN `work_order_code` VARCHAR(64) DEFAULT NULL COMMENT ''生产任务编号快照（保存时带出）''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 3. 生产任务名称快照
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'destruction_record' AND COLUMN_NAME = 'work_order_name');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `destruction_record` ADD COLUMN `work_order_name` VARCHAR(200) DEFAULT NULL COMMENT ''生产任务名称快照（前端显示）''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 4. 生产任务编号索引（按任务反查销毁台账）
-- -----------------------------------
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'destruction_record' AND INDEX_NAME = 'idx_destruction_work_order');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `destruction_record` ADD INDEX `idx_destruction_work_order` (`work_order_id`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
