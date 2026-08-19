-- =============================================
-- 修复验收单明细物料名称（material_name 取错为分类）
-- =============================================
-- 背景：
--   createAcceptanceFromOrder 旧逻辑优先取 product_name（计划复制时该字段存的是
--   "原料/辅料/包材"分类），导致自动生成的验收单明细 material_name 被错误填为分类值
--   （如"原料"、"辅料"）。物料表 material 为名称唯一权威来源。
--
-- 需手动执行：mysql -h 8.134.48.54 -P 3306 -u erp_db -p erp_db < acceptance_detail_fix_material_name.sql
--
-- 仅修正 material_name 与 material_category 相同（被填成分类值）的记录，
-- 并回填 material 表对应物料的真实名称；无匹配物料或名称一致的记录不受影响。
-- =============================================

UPDATE acceptance_detail ad
JOIN material m ON m.material_code = ad.material_code AND m.is_deleted = 0
SET ad.material_name = m.material_name
WHERE ad.is_deleted = 0
  AND ad.material_name = ad.material_category
  AND ad.material_name <> m.material_name;