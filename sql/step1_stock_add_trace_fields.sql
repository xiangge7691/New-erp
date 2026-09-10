-- ============================================
-- Step 1: 库存追溯字段 + 流水增强
-- ============================================

-- 1. Stock 表新增追溯字段
ALTER TABLE `stock`
ADD COLUMN `stock_in_id` BIGINT DEFAULT NULL COMMENT '关联入库单ID（可空，用于追溯来源）' AFTER `plan_number`,
ADD COLUMN `stock_in_detail_id` BIGINT DEFAULT NULL COMMENT '关联入库明细ID（可空）' AFTER `stock_in_id`;

-- 2. Stock 唯一索引变更
-- 先删除旧索引（如果存在）
ALTER TABLE `stock` DROP INDEX IF EXISTS `uk_stock_item_batch`;
-- 新增唯一索引：物品编码 + 仓库 + 批号 + 入库单ID + 库存状态
ALTER TABLE `stock` ADD UNIQUE KEY `uk_stock_item_batch_in` (`item_code`, `prod_unit_id`, `batch_number`, `stock_in_id`, `stock_status`);

-- 3. StockTransaction 表新增字段
ALTER TABLE `stock_transaction`
ADD COLUMN `related_order_code` VARCHAR(50) DEFAULT NULL COMMENT '关联业务单号（采购单/生产计划等）' AFTER `related_type`,
ADD COLUMN `related_order_type` VARCHAR(30) DEFAULT NULL COMMENT '业务单类型' AFTER `related_order_code`,
ADD COLUMN `item_code` VARCHAR(50) DEFAULT NULL COMMENT '物品编码' AFTER `batch_number`,
ADD COLUMN `item_name` VARCHAR(100) DEFAULT NULL COMMENT '物品名称' AFTER `item_code`,
ADD COLUMN `prod_unit_id` BIGINT DEFAULT NULL COMMENT '仓库ID' AFTER `item_name`;

-- 4. StockTransaction 索引优化
ALTER TABLE `stock_transaction`
ADD KEY `idx_stock_id` (`stock_id`),
ADD KEY `idx_transaction_date` (`transaction_date`),
ADD KEY `idx_related_type_id` (`related_type`, `related_id`);

-- 5. StockOutDetail 表新增入库单ID
ALTER TABLE `stock_out_detail`
ADD COLUMN `stock_in_id` BIGINT DEFAULT NULL COMMENT '关联入库单ID（追溯扣减来源）' AFTER `stock_id`;
