-- ============================================
-- Step 1: 库存追溯字段 + 流水增强
-- ============================================

-- 1. Stock 表新增追溯字段（如果不存在）
-- 检查并添加 stock_in_id
SET @dbname = DATABASE();
SET @tablename = 'stock';
SET @colname = 'stock_in_id';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @dbname
   AND table_name = @tablename
   AND column_name = @colname) = 0,
  CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @colname, '` BIGINT DEFAULT NULL COMMENT ''关联入库单ID（可空，用于追溯来源）'' AFTER `plan_number`'),
  'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 stock_in_detail_id
SET @colname = 'stock_in_detail_id';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @dbname
   AND table_name = @tablename
   AND column_name = @colname) = 0,
  CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @colname, '` BIGINT DEFAULT NULL COMMENT ''关联入库明细ID（可空）'' AFTER `stock_in_id`'),
  'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Stock 唯一索引变更
-- 先检查并删除旧索引（如果存在）
SET @indexname = 'uk_stock_item_batch';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = @dbname
   AND table_name = @tablename
   AND index_name = @indexname) > 0,
  CONCAT('ALTER TABLE `', @tablename, '` DROP INDEX `', @indexname, '`'),
  'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 新增唯一索引：物品编码 + 仓库 + 批号 + 入库单ID + 库存状态
SET @indexname = 'uk_stock_item_batch_in';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = @dbname
   AND table_name = @tablename
   AND index_name = @indexname) = 0,
  CONCAT('ALTER TABLE `', @tablename, '` ADD UNIQUE KEY `', @indexname, '` (`item_code`, `prod_unit_id`, `batch_number`, `stock_in_id`, `stock_status`)'),
  'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. StockTransaction 表新增字段
SET @tablename = 'stock_transaction';

-- 检查并添加 related_order_code
SET @colname = 'related_order_code';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @dbname
   AND table_name = @tablename
   AND column_name = @colname) = 0,
  CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @colname, '` VARCHAR(50) DEFAULT NULL COMMENT ''关联业务单号（采购单/生产计划等）'' AFTER `related_type`'),
  'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 related_order_type
SET @colname = 'related_order_type';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @dbname
   AND table_name = @tablename
   AND column_name = @colname) = 0,
  CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @colname, '` VARCHAR(30) DEFAULT NULL COMMENT ''业务单类型'' AFTER `related_order_code`'),
  'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 item_code
SET @colname = 'item_code';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @dbname
   AND table_name = @tablename
   AND column_name = @colname) = 0,
  CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @colname, '` VARCHAR(50) DEFAULT NULL COMMENT ''物品编码'' AFTER `batch_number`'),
  'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 item_name
SET @colname = 'item_name';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @dbname
   AND table_name = @tablename
   AND column_name = @colname) = 0,
  CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @colname, '` VARCHAR(100) DEFAULT NULL COMMENT ''物品名称'' AFTER `item_code`'),
  'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 prod_unit_id
SET @colname = 'prod_unit_id';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @dbname
   AND table_name = @tablename
   AND column_name = @colname) = 0,
  CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @colname, '` BIGINT DEFAULT NULL COMMENT ''仓库ID'' AFTER `item_name`'),
  'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. StockTransaction 索引优化
SET @tablename = 'stock_transaction';
SET @indexname = 'idx_stock_id';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = @dbname
   AND table_name = @tablename
   AND index_name = @indexname) = 0,
  CONCAT('ALTER TABLE `', @tablename, '` ADD KEY `', @indexname, '` (`stock_id`)'),
  'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @indexname = 'idx_transaction_date';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = @dbname
   AND table_name = @tablename
   AND index_name = @indexname) = 0,
  CONCAT('ALTER TABLE `', @tablename, '` ADD KEY `', @indexname, '` (`transaction_date`)'),
  'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @indexname = 'idx_related_type_id';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = @dbname
   AND table_name = @tablename
   AND index_name = @indexname) = 0,
  CONCAT('ALTER TABLE `', @tablename, '` ADD KEY `', @indexname, '` (`related_type`, `related_id`)'),
  'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. StockOutDetail 表新增入库单ID
SET @tablename = 'stock_out_detail';
SET @colname = 'stock_in_id';
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @dbname
   AND table_name = @tablename
   AND column_name = @colname) = 0,
  CONCAT('ALTER TABLE `', @tablename, '` ADD COLUMN `', @colname, '` BIGINT DEFAULT NULL COMMENT ''关联入库单ID（追溯扣减来源）'' AFTER `stock_id`'),
  'SELECT 1'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
