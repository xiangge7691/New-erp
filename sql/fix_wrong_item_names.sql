-- ============================================================
-- 修复入库单明细与库存中填错为分类值的物料名称/分类
-- 背景：验收单明细 material_name 曾被错填为分类值（"原料"/"辅料"），
--       下游 applyInbound 直接复制明细名称写入 stock_in_detail 与 stock，
--       （acceptance_detail_fix_material_name.sql 已修复验收单明细，但下游未回补）。
--       本次以物料主数据 material 为唯一权威来源，通用纠正所有不一致记录。
--       涉及物料：Y0035 乌药、Y0508 没药、F0030 蜂蜜（药用）、F0059 黄凡士林；
--       分类纠正：Y0928（应为"包材"）。
-- 执行：mysql -h8.134.48.54 -uerp_db -p89749050 erp_db < sql/fix_wrong_item_names.sql
-- 验证：
--   SELECT COUNT(*) FROM stock_in_detail d JOIN material m ON m.material_code=d.item_code AND m.is_deleted=0
--     WHERE d.item_type='material' AND d.is_deleted=0 AND d.item_name<>m.material_name;
--   SELECT COUNT(*) FROM stock s JOIN material m ON m.material_code=s.item_code AND m.is_deleted=0
--     WHERE s.item_type='material' AND s.is_deleted=0 AND s.item_name<>m.material_name;
-- ============================================================

-- 1. 修复入库单明细物料名称
UPDATE stock_in_detail d
JOIN material m ON m.material_code = d.item_code AND m.is_deleted = 0
SET d.item_name = m.material_name
WHERE d.item_type = 'material'
  AND d.is_deleted = 0
  AND d.item_name <> m.material_name;

-- 2. 修复入库单明细物料分类（防御性，幂等）
UPDATE stock_in_detail d
JOIN material m ON m.material_code = d.item_code AND m.is_deleted = 0
SET d.category_name = m.category_name
WHERE d.item_type = 'material'
  AND d.is_deleted = 0
  AND d.category_name <> m.category_name;

-- 3. 修复库存物料名称
UPDATE stock s
JOIN material m ON m.material_code = s.item_code AND m.is_deleted = 0
SET s.item_name = m.material_name
WHERE s.item_type = 'material'
  AND s.is_deleted = 0
  AND s.item_name <> m.material_name;

-- 4. 修复库存物料分类
UPDATE stock s
JOIN material m ON m.material_code = s.item_code AND m.is_deleted = 0
SET s.category_name = m.category_name
WHERE s.item_type = 'material'
  AND s.is_deleted = 0
  AND s.category_name <> m.category_name;