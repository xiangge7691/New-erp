-- ===================================
-- 物料信息表字段修改
-- ===================================

-- 1. 添加物料属性字段
ALTER TABLE `material`
ADD COLUMN `material_attribute` VARCHAR(50) DEFAULT '' COMMENT '物料属性' AFTER `category_name`;

-- 2. 添加存储要求字段
ALTER TABLE `material`
ADD COLUMN `storage_requirement` VARCHAR(500) DEFAULT '' COMMENT '存储要求' AFTER `spec`;

-- 3. 将 unit_name 字段注释修改为'计量单位'
ALTER TABLE `material`
MODIFY COLUMN `unit_name` VARCHAR(50) DEFAULT '' COMMENT '计量单位';
