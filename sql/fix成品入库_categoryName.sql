-- 修复成品入库明细的分类：将错误记录的"剂型"等值改为"成品"
-- 条件：关联的入库单 in_type = '成品入库'
UPDATE stock_in_detail sid
INNER JOIN stock_in si ON sid.in_id = si.in_id
SET sid.category_name = '成品'
WHERE si.in_type = '成品入库'
  AND sid.category_name != '成品';
