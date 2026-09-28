-- ===================================
-- 物料退货改造·数据库地基（步骤1）
-- 1) acceptance_detail 增加明细级状态与退货相关列
-- 2) purchase_order_items 增加与验收共享的明细状态列
-- 3) 新建重新发货操作留痕表 acceptance_resend_log
-- 4) 存量数据回填：明细状态按主单状态推导、采购明细状态按订单状态推导、
--    补齐 acceptance_detail.purchase_item_id 关联锚点
-- 说明：全部幂等，可重复执行
-- ===================================

-- 1. acceptance_detail 增列（幂等）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_detail' AND COLUMN_NAME = 'status');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_detail` ADD COLUMN `status` VARCHAR(20) DEFAULT NULL COMMENT ''明细状态：待初验/待检验/待入库/待退货/已退货/已重发/已入库/已取消'' AFTER `diff_quantity`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_detail' AND COLUMN_NAME = 'return_reason');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_detail` ADD COLUMN `return_reason` VARCHAR(20) DEFAULT NULL COMMENT ''退货原因：初验不合格/检验不合格（验收环节带出，退货页只读）'' AFTER `status`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_detail' AND COLUMN_NAME = 'return_remark');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_detail` ADD COLUMN `return_remark` VARCHAR(500) DEFAULT NULL COMMENT ''不合格原因说明（验收环节填写，退货页只读展示）'' AFTER `return_reason`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_detail' AND COLUMN_NAME = 'supplier');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_detail` ADD COLUMN `supplier` VARCHAR(100) DEFAULT NULL COMMENT ''供应商（从采购订单明细自动带出）'' AFTER `return_remark`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_detail' AND COLUMN_NAME = 'original_seq');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_detail` ADD COLUMN `original_seq` INT DEFAULT NULL COMMENT ''部分验收拆行时记录的原明细序号（用于追溯）'' AFTER `supplier`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_detail' AND COLUMN_NAME = 'prod_unit_id');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_detail` ADD COLUMN `prod_unit_id` BIGINT DEFAULT NULL COMMENT ''明细级入库仓库（生产单位ID，每条物料独立选择）'' AFTER `original_seq`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_detail' AND COLUMN_NAME = 'purchase_item_id');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `acceptance_detail` ADD COLUMN `purchase_item_id` BIGINT DEFAULT NULL COMMENT ''关联采购订单明细ID（明细状态同步锚点）'' AFTER `prod_unit_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. purchase_order_items 增加共享明细状态列（幂等）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_order_items' AND COLUMN_NAME = 'status');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `purchase_order_items` ADD COLUMN `status` VARCHAR(20) DEFAULT NULL COMMENT ''明细状态（与验收共享）：待初验/待检验/待入库/待退货/已退货/已重发/已入库/已取消'' AFTER `difference`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 重新发货操作留痕表（幂等）
CREATE TABLE IF NOT EXISTS `acceptance_resend_log` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `acceptance_id`       BIGINT       NOT NULL COMMENT '来源验收单ID（退货明细所属验收单）',
  `detail_id`           BIGINT       NOT NULL COMMENT '来源验收明细ID',
  `material_name`       VARCHAR(100) DEFAULT NULL COMMENT '物料名称（冗余，便于留痕展示）',
  `resend_qty`          DECIMAL(18,3) NOT NULL COMMENT '重新发货数量',
  `new_acceptance_code` VARCHAR(50)  NOT NULL COMMENT '重新发货生成的新验收单号',
  `operation_time`      DATETIME     NOT NULL COMMENT '重新发货操作时间',
  `created_by`          BIGINT       DEFAULT NULL COMMENT '操作人ID',
  `created_time`        DATETIME     DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_acceptance_id` (`acceptance_id`),
  KEY `idx_detail_id` (`detail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验收明细重新发货操作留痕表';

-- 4. 回填 acceptance_detail.status（仅回填空值，按存量主单状态推导，幂等）
UPDATE `acceptance_detail` d
JOIN `acceptance_order` o ON d.`acceptance_id` = o.`acceptance_id`
SET d.`status` = CASE o.`status`
    WHEN '运输中'   THEN '待初验'
    WHEN '到货初验' THEN '待初验'
    WHEN '物料检验' THEN '待检验'
    WHEN '待退货'   THEN '待退货'
    WHEN '已入库'   THEN '已入库'
    WHEN '已退换'   THEN '已重发'
    WHEN '已退货'   THEN '已取消'
    ELSE '待初验'
END
WHERE d.`status` IS NULL;

-- 5. 回填 acceptance_detail.purchase_item_id（按 采购订单号+明细序号 匹配，幂等）
--    仅匹配未拆行的原始明细（original_seq IS NULL），拆分子行由业务代码维护关联
UPDATE `acceptance_detail` d
JOIN `acceptance_order` o ON d.`acceptance_id` = o.`acceptance_id`
JOIN `purchase_orders` po ON po.`purchase_number` = COALESCE(o.`purchase_number`, o.`related_order`)
JOIN `purchase_order_items` poi ON poi.`order_id` = po.`id` AND poi.`sequence_number` = d.`seq`
SET d.`purchase_item_id` = poi.`id`
WHERE d.`purchase_item_id` IS NULL
  AND d.`original_seq` IS NULL;

-- 6. 回填 purchase_order_items.status（仅回填空值，按存量订单主单状态推导，幂等）
UPDATE `purchase_order_items` poi
JOIN `purchase_orders` po ON poi.`order_id` = po.`id`
SET poi.`status` = CASE po.`status`
    WHEN '运输中'   THEN '待初验'
    WHEN '到货初验' THEN '待初验'
    WHEN '物料检验' THEN '待检验'
    WHEN '待退货'   THEN '待退货'
    WHEN '已入库'   THEN '已入库'
    WHEN '已退换'   THEN '已重发'
    WHEN '已退货'   THEN '已取消'
    ELSE NULL
END
WHERE poi.`status` IS NULL
  AND po.`status` <> '待采购';
