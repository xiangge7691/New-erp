-- ===================================
-- 采购计划采购进度（current_status / current_status_date）全量校正脚本
-- 背景：采购计划页需展示【运输中/验收中/已入库/已结束/已作废】等状态，
--       取值派生自关联采购订单状态；功能上线前生成的计划、或订单状态由非标准入口改写的记录，
--       进度可能为空或滞后，需按订单当前状态校正。
-- 说明：两个数据库（8.148.27.237/erp_db1、8.134.48.54/erp_db）通用，全部幂等，可重复执行。
--       只校正"进度与订单状态不一致"的行，不触碰一致行，不修改任何订单数据。
-- 执行顺序：① 增列（若缺）→ ② 存量回填 → ③ 校验
-- ===================================

-- -----------------------------------
-- ① 增列（幂等）：purchase_plan.current_status / current_status_date
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_plan' AND COLUMN_NAME = 'current_status');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `purchase_plan` ADD COLUMN `current_status` VARCHAR(20) DEFAULT NULL COMMENT ''采购进度（派生自关联采购订单状态）'' AFTER `purchase_order_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_plan' AND COLUMN_NAME = 'current_status_date');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `purchase_plan` ADD COLUMN `current_status_date` DATETIME DEFAULT NULL COMMENT ''采购进度更新日期'' AFTER `current_status`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- ② 存量回填：按关联采购订单当前状态校正进度（仅不一致行）
--    注意：两个库 purchase_plan 与 purchase_orders 的 status 列排序规则可能不同
--          （utf8mb4_general_ci / utf8mb4_0900_ai_ci），比较时必须显式 COLLATE，否则报
--          Illegal mix of collations
-- -----------------------------------
UPDATE `purchase_plan` p
JOIN `purchase_orders` o ON o.`id` = p.`purchase_order_id` AND o.`is_deleted` = 0
SET p.`current_status` = o.`status`,
    p.`current_status_date` = NOW()
WHERE p.`is_deleted` = 0
  AND o.`status` IS NOT NULL
  AND CONVERT(IFNULL(p.`current_status`, '') USING utf8mb4) COLLATE utf8mb4_general_ci
      <> CONVERT(o.`status` USING utf8mb4) COLLATE utf8mb4_general_ci;

-- -----------------------------------
-- ③ 校验：应返回 0 行
-- -----------------------------------
SELECT p.`plan_code`,
       CONVERT(IFNULL(p.`current_status`, 'NULL') USING utf8mb4) AS plan_progress,
       CONVERT(o.`status` USING utf8mb4)                       AS order_status
FROM `purchase_plan` p
JOIN `purchase_orders` o ON o.`id` = p.`purchase_order_id` AND o.`is_deleted` = 0
WHERE p.`is_deleted` = 0
  AND CONVERT(IFNULL(p.`current_status`, '') USING utf8mb4) COLLATE utf8mb4_general_ci
      <> CONVERT(IFNULL(o.`status`, '') USING utf8mb4) COLLATE utf8mb4_general_ci;
