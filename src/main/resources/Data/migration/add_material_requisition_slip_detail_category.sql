-- ===================================
-- 领料单明细增加物料分类列（category_name）
-- 背景：领料单明细需要展示物料分类（原料/辅料/包材），需单独落库
-- 说明：全部幂等，可重复执行
-- ===================================

-- material_requisition_slip_detail 增列（幂等）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_requisition_slip_detail' AND COLUMN_NAME = 'category_name');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `material_requisition_slip_detail` ADD COLUMN `category_name` VARCHAR(50) DEFAULT NULL COMMENT ''物料分类(原料/辅料/包材)'' AFTER `material_name`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量回填：明细分类为空时，按物料编码从物料主数据取分类（幂等）
UPDATE `material_requisition_slip_detail` d
JOIN `material` m ON m.`material_code` = d.`material_code` AND m.`is_deleted` = 0
SET d.`category_name` = m.`category_name`
WHERE (d.`category_name` IS NULL OR d.`category_name` = '');
