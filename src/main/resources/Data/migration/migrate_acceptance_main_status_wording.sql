-- ===================================
-- 主单状态词迁移（旧词 → 新词表）
-- 适用：货物验收模块三模块改造（采购订单/货物验收/物料退货管理）
-- 说明：
--   1. 必须在明细状态回填脚本（add_acceptance_return_detail_status.sql）之后执行，
--      因为明细回填以主单旧词为输入
--   2. 旧词映射：到货初验/物料检验 → 验收中；待退货/已退换/已退货 → 已结束
--      （与 AcceptanceStatusPolicy.deriveMainStatus 派生规则一致）
--   3. 幂等：UPDATE 条件迁移后不再匹配，可重复执行
-- 执行方式：在 erp_db 数据库直接执行
-- ===================================

-- 1. 验收单主单状态词迁移
UPDATE `acceptance_order` SET `status` = '验收中'
WHERE `status` IN ('到货初验', '物料检验');

UPDATE `acceptance_order` SET `status` = '已结束'
WHERE `status` IN ('待退货', '已退换', '已退货');

-- 2. 采购订单主单状态词迁移（待采购/运输中/已入库/已结束/已作废 保持不变）
UPDATE `purchase_orders` SET `status` = '验收中'
WHERE `status` IN ('到货初验', '物料检验');

UPDATE `purchase_orders` SET `status` = '已结束'
WHERE `status` IN ('待退货', '已退换', '已退货');

-- 3. 存量列修正：acceptance_order.status 默认值与注释切换为新词表
--    （CREATE IF NOT EXISTS 不更新已存在表，幂等判断）
SET @def := (SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'acceptance_order' AND COLUMN_NAME = 'status');
SET @ddl := IF(@def LIKE '%到货初验%',
    'ALTER TABLE `acceptance_order` MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT ''运输中'' COMMENT ''状态：运输中/验收中/已入库/已结束/已作废''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
