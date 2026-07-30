-- ===================================
-- 制剂工序模板表新增字段
-- ===================================

-- 新增关键工艺参数字段
ALTER TABLE `preparation_process_template`
ADD COLUMN `key_process_params` TEXT DEFAULT NULL COMMENT '关键工艺参数' AFTER `equipment_params`;
