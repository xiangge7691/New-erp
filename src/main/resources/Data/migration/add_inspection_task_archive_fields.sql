-- ===================================
-- 检验任务归档字段
-- 1) inspection_task 增加 archive_time、archive_by
-- 2) 更新 task_status 注释，补充“已归档”
-- 说明：
--   1. 新增列全部通过 information_schema 判断，幂等，可重复执行
--   2. 不指定 AFTER，新增列追加到表尾，避免线上大表因列位置调整触发表重建
-- 执行库：8.134.48.54/erp_db（测试）、8.148.27.237/erp_db1（生产）
-- ===================================

-- -----------------------------------
-- 1. 归档时间
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_task' AND COLUMN_NAME = 'archive_time');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `inspection_task` ADD COLUMN `archive_time` DATETIME DEFAULT NULL COMMENT ''归档时间''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 2. 归档人ID
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'inspection_task' AND COLUMN_NAME = 'archive_by');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `inspection_task` ADD COLUMN `archive_by` BIGINT DEFAULT NULL COMMENT ''归档人ID''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 3. 任务状态注释补充“已归档”
-- -----------------------------------
ALTER TABLE `inspection_task`
  MODIFY COLUMN `task_status` VARCHAR(20) NOT NULL
    COMMENT '任务状态：待开始/进行中/已完成/已作废/已归档';
