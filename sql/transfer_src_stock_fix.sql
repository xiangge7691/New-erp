-- ============================================================
-- 修复调拨"只增不减"问题产生的历史错误数据
-- 背景：调空源库存时原代码使用 setIsDeleted(1)+updateById，
--       全局软删除配置下 updateById 不会将 is_deleted 放入 SET 子句，
--       导致源库存行未被软删除（表现为"调拨只增加没减"）。
-- 修复：凡调拨数量=源库存数量（全额调拨）的源库存行，若仍为有效状态则补软删除。
-- 执行：mysql -h8.134.48.54 -uerp_db -p89749050 erp_db < sql/transfer_src_stock_fix.sql
-- ============================================================

UPDATE stock s
JOIN transfer_order_detail d ON d.src_stock_id = s.stock_id
SET s.is_deleted = 1
WHERE s.is_deleted = 0
  AND d.transfer_quantity = d.src_stock;