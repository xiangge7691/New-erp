-- ============================================================
-- 移除 room_info 表冗余的文件字段
-- 文件关联已通过 file_info 表的 businessType + businessId 实现
-- ============================================================

ALTER TABLE `room_info`
  DROP COLUMN `clean_procedure_file_id`,
  DROP COLUMN `disinfection_procedure`,
  DROP COLUMN `clean_inspection_procedure`;
