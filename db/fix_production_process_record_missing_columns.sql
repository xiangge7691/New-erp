-- ============================================================
-- 修复 production_process_record 表缺少 process_type_id 列
-- is_deleted 和 version 已在之前添加
-- ============================================================

ALTER TABLE `production_process_record` 
  ADD COLUMN `process_type_id` BIGINT DEFAULT NULL COMMENT '工序类型ID' AFTER `plan_id`;
