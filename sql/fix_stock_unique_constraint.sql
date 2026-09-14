-- ===================================
-- 删除旧的唯一索引（同仓库同批号唯一）
-- 保留新的唯一索引 uk_stock_item_batch_in（含入库单ID）
-- 执行时间: 2026-09-11
-- ===================================

-- 检查旧索引是否存在
SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics 
                     WHERE table_schema = DATABASE() 
                     AND table_name = 'stock' 
                     AND index_name = 'uk_stock_unique');

-- 如果存在则删除
SET @sql = IF(@index_exists > 0, 'DROP INDEX uk_stock_unique ON stock', 'SELECT "索引不存在"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
