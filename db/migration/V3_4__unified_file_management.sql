-- ===================================
-- V4: 统一文件管理 - 通过 businessType + businessId 关联
-- ===================================

-- 1. file_info 表新增 custom_path 字段
ALTER TABLE file_info ADD COLUMN custom_path VARCHAR(500) DEFAULT NULL COMMENT '自定义存储子目录';

-- 2. personnel_certificate 表删除 file_id
ALTER TABLE personnel_certificate DROP COLUMN file_id;

-- 3. personnel_file 表删除单文件字段
ALTER TABLE personnel_file DROP COLUMN health_file;
ALTER TABLE personnel_file DROP COLUMN attachments;
