-- ===================================
-- 修正领料单自动生成验收单的明细物料分类
-- 背景：领料单生成货物验收单时曾硬编码 material_category = '物料'，
--       导致验收明细丢失真实分类（原料/辅料/包材）。
-- 处理：优先按物料编码从物料主数据（material.category_name）回填，
--       主数据缺失时回退领料明细已回填的分类；全部幂等，可重复执行。
-- ===================================

-- 1) 以物料主数据为准修正
UPDATE `acceptance_detail` d
JOIN `material` m ON m.`material_code` = d.`material_code` AND m.`is_deleted` = 0
SET d.`material_category` = m.`category_name`
WHERE d.`material_category` = '物料'
  AND m.`category_name` IS NOT NULL
  AND m.`category_name` <> '';

-- 2) 物料主数据缺失时，回退领料明细上的分类
UPDATE `acceptance_detail` d
JOIN `acceptance_order` o ON o.`acceptance_id` = d.`acceptance_id`
JOIN `material_requisition_slip_detail` sd
  ON sd.`slip_id` = o.`related_slip_id` AND sd.`material_code` = d.`material_code`
SET d.`material_category` = sd.`category_name`
WHERE d.`material_category` = '物料'
  AND sd.`category_name` IS NOT NULL
  AND sd.`category_name` <> '';
