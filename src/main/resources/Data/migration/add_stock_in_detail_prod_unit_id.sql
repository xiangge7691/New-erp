-- ===================================
-- 物料退货改造·步骤4（货物验收行级化）
-- stock_in_detail 增加明细级入库仓库列（prod_unit_id）
-- 背景：验收入库改造为"每条待入库明细独立选择仓库"，入库单明细需记录各自仓库，
--       主表 stock_in.prod_unit_id 取第一条明细的仓库
-- 说明：全部幂等，可重复执行
-- ===================================

-- stock_in_detail 增列（幂等）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_in_detail' AND COLUMN_NAME = 'prod_unit_id');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `stock_in_detail` ADD COLUMN `prod_unit_id` BIGINT DEFAULT NULL COMMENT ''明细级入库仓库（生产单位ID，每条明细独立选择）'' AFTER `in_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量回填：明细仓库为空时，沿用主表 stock_in.prod_unit_id（幂等）
UPDATE `stock_in_detail` d
JOIN `stock_in` s ON d.`in_id` = s.`in_id`
SET d.`prod_unit_id` = s.`prod_unit_id`
WHERE d.`prod_unit_id` IS NULL
  AND s.`prod_unit_id` IS NOT NULL;