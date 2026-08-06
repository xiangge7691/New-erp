-- ===================================
-- 删除库存相关数据库触发器（逻辑已迁移至后端）
-- 说明：
--   1. 以下4个触发器原本在数据库层完成"单据确认/取消时的库存联动"
--      （更新 stock 表 + 写入 stock_transaction 流水）
--   2. 该逻辑已迁移到后端 Java 实现：
--      - 入库确认：POST /api/stockin/{id}/confirm  （StockInServiceImpl.confirmStockIn）
--      - 入库取消：POST /api/stockin/{id}/cancel    （StockInServiceImpl.cancelStockIn）
--      - 出库确认：POST /api/stockout/{id}/confirm （StockOutServiceImpl.confirmStockOut）
--      - 出库取消：POST /api/stockout/{id}/cancel   （StockOutServiceImpl.cancelStockOut）
--      - 公共库存联动：StockServiceImpl.applyInbound/applyOutbound/rollbackInbound/rollbackOutbound
--   3. 后端实现相对触发器更准确：库存按"物品编码+仓库+批号"为唯一键（原触发器按
--      物品ID合并批次），库存状态使用"合格/待检/不合格"中文值，流水类型使用中文
--      入库/出库类型，与前端页面需求一致
-- 执行方式：在 erp_db 数据库直接执行本脚本（可重复执行，IF EXISTS 幂等）
-- ===================================

-- 入库单确认时的库存联动触发器（已迁移至后端 confirmStockIn）
DROP TRIGGER IF EXISTS `after_stock_in_confirm`;

-- 入库单取消时的库存回滚触发器（已迁移至后端 cancelStockIn）
DROP TRIGGER IF EXISTS `after_stock_in_cancel`;

-- 出库单确认时的库存扣减触发器（已迁移至后端 confirmStockOut）
DROP TRIGGER IF EXISTS `after_stock_out_confirm`;

-- 出库单取消时的库存回滚触发器（已迁移至后端 cancelStockOut）
DROP TRIGGER IF EXISTS `after_stock_out_cancel`;
