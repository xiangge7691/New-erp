-- ===================================
-- 剂型表字段修改 + 制剂表新增剂型字段
-- ===================================

-- 1. 剂型表：dosage_name 改名为 dosage_category
ALTER TABLE `dosage_form`
CHANGE COLUMN `dosage_name` `dosage_category` VARCHAR(50) NOT NULL COMMENT '剂型大类';

-- 2. 剂型表：新增 dosage_name 字段
ALTER TABLE `dosage_form`
ADD COLUMN `dosage_name` VARCHAR(50) DEFAULT '' COMMENT '剂型名称' AFTER `dosage_category`;

-- 3. 制剂表：dosage_form 改名为 dosage_category
ALTER TABLE `preparation`
CHANGE COLUMN `dosage_form` `dosage_category` VARCHAR(50) DEFAULT '' COMMENT '剂型大类';

-- 4. 制剂表：新增 dosage_name 字段
ALTER TABLE `preparation`
ADD COLUMN `dosage_name` VARCHAR(50) DEFAULT '' COMMENT '剂型名称' AFTER `dosage_category`;
