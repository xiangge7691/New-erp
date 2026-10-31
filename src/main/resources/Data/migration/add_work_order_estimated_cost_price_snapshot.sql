-- ===================================
-- 首页看板 V2：工单预计成本价快照字段
-- 背景：看板「总预计成本 = 预计成本价 × 生产数量」，若价格实时读取制剂信息，
--       制剂改价会导致历史月份数据漂移，故在工单创建时冻结快照。
-- 说明：
--   1. 全部通过 information_schema 判断，幂等，可重复执行
--   2. 仅新增可空列，不修改任何已有列、不删除任何数据
--   3. 回填只写新增列，且仅在制剂已维护预计成本价时生效，不覆盖已有快照
-- 执行库：8.134.48.54/erp_db（测试）、8.148.27.237/erp_db1（生产）
-- 已执行：2026-10-31 生产库 erp_db1（新增列成功，回填命中 0 行，原有数据零变化）
-- ===================================

-- -----------------------------------
-- ① 增列（幂等）：work_order.estimated_cost_price_snapshot
-- -----------------------------------
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'work_order'
      AND COLUMN_NAME = 'estimated_cost_price_snapshot');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `work_order` ADD COLUMN `estimated_cost_price_snapshot` DECIMAL(18,4) DEFAULT NULL COMMENT ''预计成本价快照（元/件），工单创建时冻结'' AFTER `sales_price`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- ② 存量回填（幂等）：仅补空值，不覆盖已有快照
-- -----------------------------------
UPDATE `work_order` w
JOIN `preparation` p
  ON p.`preparation_code` = w.`preparation_code`
 AND p.`is_deleted` = 0
SET w.`estimated_cost_price_snapshot` = p.`estimated_cost_price`
WHERE w.`is_deleted` = 0
  AND w.`estimated_cost_price_snapshot` IS NULL
  AND p.`estimated_cost_price` IS NOT NULL
  AND p.`estimated_cost_price` <> 0;

-- -----------------------------------
-- ③ 校验（只读）：快照填充率
-- -----------------------------------
SELECT COUNT(*) AS active_total,
       SUM(`estimated_cost_price_snapshot` IS NULL) AS snapshot_null,
       SUM(`estimated_cost_price_snapshot` IS NOT NULL) AS snapshot_not_null
FROM `work_order`
WHERE `is_deleted` = 0;
