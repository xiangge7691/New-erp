-- ===================================
-- 货物验收·取样/留样数量扣减
-- 1) acceptance_detail 增加取样数量（sampling_qty）、留样数量（retention_qty）
-- 2) 存量回填：已生成入库单/已入库且入库数量为空的行，按实际到货数量（回退采购数量）回填
-- 说明：
--   1. 全部通过 information_schema 判断，幂等，可重复执行
--   2. 不指定 AFTER，新增列追加到表尾，避免大表因列位置调整触发表重建
--   3. 入库数量 = 基准数量（实际到货数量，未填时回退采购数量）− 取样数量 − 留样数量，
--      由服务端计算并回写 inbound_qty，取样/留样不进入库存
-- 执行库：192.168.5.100/erp_db（测试）
-- ===================================

-- -----------------------------------
-- 1. acceptance_detail 增加取样数量
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_detail' AND COLUMN_NAME = 'sampling_qty');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_detail` ADD COLUMN `sampling_qty` DECIMAL(18,3) DEFAULT NULL COMMENT ''取样数量（验收时抽取的检验样品数量，不计入入库；空按0处理）''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 2. acceptance_detail 增加留样数量
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_detail' AND COLUMN_NAME = 'retention_qty');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_detail` ADD COLUMN `retention_qty` DECIMAL(18,3) DEFAULT NULL COMMENT ''留样数量（验收时留存样品数量，不计入入库；空按0处理）''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 3. 存量回填：已生成入库单/已入库但入库数量为空的行
-- -----------------------------------
UPDATE `acceptance_detail`
SET `inbound_qty` = COALESCE(`actual_arrival_qty`, `quantity`)
WHERE `inbound_qty` IS NULL
  AND `status` IN ('待扫码入库', '已入库')
  AND `is_deleted` = 0;
