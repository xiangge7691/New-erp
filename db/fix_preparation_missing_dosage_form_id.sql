-- ============================================================
-- 修复 preparation 表缺少 dosage_form_id 列
-- ============================================================

ALTER TABLE `preparation` 
  ADD COLUMN `dosage_form_id` BIGINT DEFAULT NULL COMMENT '剂型ID' AFTER `dosage_form`;
