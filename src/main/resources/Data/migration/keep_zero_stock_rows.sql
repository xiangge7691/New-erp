-- ===================================
-- 库存批次归零改为保留记录（不再逻辑删除）
-- 背景：出库/调拨清零时旧逻辑执行 deleteById（is_deleted=1），
--       导致库存查询看不到、按批次查流水也查不到历史出入库记录。
-- 处理：将“因清零而被逻辑删除”的库存行恢复为正常行，数量置 0；
--       后续查询默认过滤 quantity > 0，需要时可用 showZero 查看历史批次。
-- 说明：只恢复存在清零流水（quantity_after = 0）的行，不影响删除仓库等场景；幂等，可重复执行。
-- ===================================

UPDATE `stock` s
SET s.`is_deleted` = 0,
    s.`quantity` = 0
WHERE s.`is_deleted` = 1
  AND EXISTS (
    SELECT 1
    FROM `stock_transaction` t
    WHERE t.`stock_id` = s.`stock_id`
      AND t.`related_type` IN ('stock_out', 'transfer')
      AND t.`quantity_after` = 0
  );
