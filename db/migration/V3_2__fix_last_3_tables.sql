-- ===================================
-- V3 补充脚本 2：修复剩余 3 个表
-- ===================================

-- ===================================
-- 1. equipment: 删除索引后重命名列
-- ===================================
ALTER TABLE equipment DROP INDEX idx_creator_id;
ALTER TABLE equipment CHANGE COLUMN creator_id created_by bigint unsigned DEFAULT NULL COMMENT '创建人ID';
ALTER TABLE equipment CHANGE COLUMN updater_id updated_by bigint unsigned DEFAULT NULL COMMENT '修改人ID';

-- ===================================
-- 2. production_process_record: 删除索引后重命名列
-- ===================================
ALTER TABLE production_process_record DROP INDEX idx_creator_id;
ALTER TABLE production_process_record CHANGE COLUMN creator_id created_by bigint unsigned DEFAULT NULL COMMENT '创建人ID';
ALTER TABLE production_process_record CHANGE COLUMN updater_id updated_by bigint unsigned DEFAULT NULL COMMENT '修改人ID';

-- ===================================
-- 3. user_role: 补充 created_by
-- ===================================
ALTER TABLE user_role ADD COLUMN created_by bigint DEFAULT NULL COMMENT '创建人ID' AFTER role_id;
