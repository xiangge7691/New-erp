-- ============================================================
-- ERP数据库一键修复脚本（不删库）
-- 执行前请务必备份数据库！
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 第0步：关闭严格模式，允许修复无效日期
-- ============================================================

SET @old_sql_mode = @@sql_mode;
SET sql_mode = '';

-- ============================================================
-- 第1步：修复无效日期数据
-- ============================================================

UPDATE `purchase_orders` SET `expected_delivery_date` = NULL WHERE `expected_delivery_date` = '0000-00-00';
UPDATE `purchase_orders` SET `processing_date` = NULL WHERE `processing_date` = '0000-00-00';
UPDATE `stock_in` SET `in_date` = NULL WHERE `in_date` = '0000-00-00';
UPDATE `stock_out` SET `out_date` = NULL WHERE `out_date` = '0000-00-00';
UPDATE `equipment` SET `purchase_date` = NULL WHERE `purchase_date` = '0000-00-00';
UPDATE `equipment` SET `last_maintenance_date` = NULL WHERE `last_maintenance_date` = '0000-00-00';
UPDATE `equipment_maintenance` SET `maintenance_date` = NULL WHERE `maintenance_date` = '0000-00-00';
UPDATE `equipment_maintenance` SET `next_maintenance_date` = NULL WHERE `next_maintenance_date` = '0000-00-00';
UPDATE `personnel_file` SET `entry_date` = NULL WHERE `entry_date` = '0000-00-00';
UPDATE `personnel_file` SET `health_cert_issue` = NULL WHERE `health_cert_issue` = '0000-00-00';
UPDATE `personnel_file` SET `health_cert_expire` = NULL WHERE `health_cert_expire` = '0000-00-00';
UPDATE `personnel_file` SET `last_checkup_date` = NULL WHERE `last_checkup_date` = '0000-00-00';
UPDATE `preparation_document` SET `effective_date` = NULL WHERE `effective_date` = '0000-00-00';
UPDATE `preparation_document` SET `expire_date` = NULL WHERE `expire_date` = '0000-00-00';
UPDATE `stock` SET `production_date` = NULL WHERE `production_date` = '0000-00-00';
UPDATE `stock` SET `expiry_date` = NULL WHERE `expiry_date` = '0000-00-00';
UPDATE `stock_in_detail` SET `production_date` = NULL WHERE `production_date` = '0000-00-00';
UPDATE `stock_in_detail` SET `expiry_date` = NULL WHERE `expiry_date` = '0000-00-00';

-- ============================================================
-- 第2步：ID字段类型统一为BIGINT
-- ============================================================

-- 动态删除purchase_order_items的外键
SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'purchase_order_items' 
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    AND CONSTRAINT_NAME = 'purchase_order_items_ibfk_1');
SET @sql = IF(@constraint_exists > 0, 
    'ALTER TABLE `purchase_order_items` DROP FOREIGN KEY `purchase_order_items_ibfk_1`', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- purchase_orders.id: INT → BIGINT
ALTER TABLE `purchase_orders` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';

-- purchase_order_items.id: INT → BIGINT
ALTER TABLE `purchase_order_items` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';

-- purchase_order_items.order_id: INT → BIGINT
ALTER TABLE `purchase_order_items` MODIFY COLUMN `order_id` BIGINT NOT NULL COMMENT '关联订单ID';

-- 重新添加外键约束
ALTER TABLE `purchase_order_items` ADD CONSTRAINT `fk_poi_order` FOREIGN KEY (`order_id`) REFERENCES `purchase_orders` (`id`);

-- ============================================================
-- 第3步：主键命名统一
-- ============================================================

-- user_role: 先删除外键
SET @fk_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'user_role' 
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    AND CONSTRAINT_NAME = 'fk_user_role_role');
SET @sql = IF(@fk_exists > 0, 
    'ALTER TABLE `user_role` DROP FOREIGN KEY `fk_user_role_role`', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'user_role' 
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    AND CONSTRAINT_NAME = 'fk_user_role_user');
SET @sql = IF(@fk_exists > 0, 
    'ALTER TABLE `user_role` DROP FOREIGN KEY `fk_user_role_user`', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- user_role表: userrole_id → id
ALTER TABLE `user_role` CHANGE COLUMN `userrole_id` `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID';

-- 重新添加外键
ALTER TABLE `user_role` ADD CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE;
ALTER TABLE `user_role` ADD CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

-- role_perm: 先删除外键
SET @fk_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'role_perm' 
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    AND CONSTRAINT_NAME = 'fk_role_perm_perm');
SET @sql = IF(@fk_exists > 0, 
    'ALTER TABLE `role_perm` DROP FOREIGN KEY `fk_role_perm_perm`', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'role_perm' 
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    AND CONSTRAINT_NAME = 'fk_role_perm_role');
SET @sql = IF(@fk_exists > 0, 
    'ALTER TABLE `role_perm` DROP FOREIGN KEY `fk_role_perm_role`', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- role_perm表: roleperm_id → id
ALTER TABLE `role_perm` CHANGE COLUMN `roleperm_id` `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID';

-- 重新添加外键
ALTER TABLE `role_perm` ADD CONSTRAINT `fk_role_perm_perm` FOREIGN KEY (`perm_id`) REFERENCES `permission` (`perm_id`) ON DELETE CASCADE;
ALTER TABLE `role_perm` ADD CONSTRAINT `fk_role_perm_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE;

-- user_department: 先删除外键
SET @fk_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'user_department' 
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    AND CONSTRAINT_NAME = 'fk_user_department_dept');
SET @sql = IF(@fk_exists > 0, 
    'ALTER TABLE `user_department` DROP FOREIGN KEY `fk_user_department_dept`', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'user_department' 
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    AND CONSTRAINT_NAME = 'fk_user_department_user');
SET @sql = IF(@fk_exists > 0, 
    'ALTER TABLE `user_department` DROP FOREIGN KEY `fk_user_department_user`', 
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- user_department表: userdepartment_id → id
ALTER TABLE `user_department` CHANGE COLUMN `userdepartment_id` `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID';

-- 重新添加外键
ALTER TABLE `user_department` ADD CONSTRAINT `fk_user_department_dept` FOREIGN KEY (`department_id`) REFERENCES `department` (`department_id`) ON DELETE CASCADE;
ALTER TABLE `user_department` ADD CONSTRAINT `fk_user_department_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE;

-- ============================================================
-- 第4步：补充 is_deleted 字段
-- ============================================================

ALTER TABLE `user_role` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `created_at`;
ALTER TABLE `role_perm` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `created_at`;
ALTER TABLE `user_department` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `created_at`;

-- ============================================================
-- 第5步：department表新增缺失字段
-- ============================================================

ALTER TABLE `department` ADD COLUMN `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID（0为顶级）' AFTER `department_name`;
ALTER TABLE `department` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用' AFTER `parent_id`;
ALTER TABLE `department` ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号' AFTER `status`;
ALTER TABLE `department` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `sort_order`;
ALTER TABLE `department` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;
ALTER TABLE `department` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- ============================================================
-- 第6步：unit表新增缺失字段
-- ============================================================

ALTER TABLE `unit` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用' AFTER `symbol`;
ALTER TABLE `unit` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `status`;
ALTER TABLE `unit` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;
ALTER TABLE `unit` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- ============================================================
-- 第7步：dosage_form表新增缺失字段
-- ============================================================

ALTER TABLE `dosage_form` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用' AFTER `remark`;
ALTER TABLE `dosage_form` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `status`;
ALTER TABLE `dosage_form` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;
ALTER TABLE `dosage_form` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- ============================================================
-- 第8步：material表新增缺失字段
-- ============================================================

ALTER TABLE `material` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_time`;
ALTER TABLE `material` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第9步：permission表新增缺失字段
-- ============================================================

ALTER TABLE `permission` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `perm_status`;
ALTER TABLE `permission` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第10步：role表新增缺失字段
-- ============================================================

ALTER TABLE `role` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `role_status`;
ALTER TABLE `role` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第11步：room_info表新增缺失字段
-- ============================================================

ALTER TABLE `room_info` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_time`;
ALTER TABLE `room_info` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第12步：equipment表新增缺失字段
-- ============================================================

ALTER TABLE `equipment` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_time`;
ALTER TABLE `equipment` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第13步：process_type表新增缺失字段
-- ============================================================

ALTER TABLE `process_type` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_time`;
ALTER TABLE `process_type` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第14步：production_unit表新增缺失字段
-- ============================================================

ALTER TABLE `production_unit` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `update_time`;
ALTER TABLE `production_unit` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第15步：prod_unit_invoice表新增缺失字段
-- ============================================================

ALTER TABLE `prod_unit_invoice` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `update_time`;

-- ============================================================
-- 第16步：prod_unit_material_file表新增缺失字段
-- ============================================================

ALTER TABLE `prod_unit_material_file` ADD COLUMN `file_path` VARCHAR(500) DEFAULT NULL COMMENT '文件存储路径' AFTER `file_size`;
ALTER TABLE `prod_unit_material_file` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `created_time`;

-- ============================================================
-- 第17步：preparation表新增缺失字段
-- ============================================================

ALTER TABLE `preparation` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_time`;
ALTER TABLE `preparation` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第18步：preparation_formula表新增缺失字段
-- ============================================================

ALTER TABLE `preparation_formula` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_time`;

-- ============================================================
-- 第19步：production_plan表新增缺失字段
-- ============================================================

ALTER TABLE `production_plan` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `update_time`;
ALTER TABLE `production_plan` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第20步：work_order表新增缺失字段
-- ============================================================

ALTER TABLE `work_order` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `return_amount`;
ALTER TABLE `work_order` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第21步：stock表新增缺失字段
-- ============================================================

ALTER TABLE `stock` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_by`;
ALTER TABLE `stock` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第22步：stock_in表新增缺失字段
-- ============================================================

ALTER TABLE `stock_in` ADD COLUMN `approval_instance_id` BIGINT DEFAULT NULL COMMENT '审批实例ID' AFTER `remark`;
ALTER TABLE `stock_in` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_time`;
ALTER TABLE `stock_in` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第23步：stock_in_detail表新增缺失字段
-- ============================================================

ALTER TABLE `stock_in_detail` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `remark`;

-- ============================================================
-- 第24步：stock_out表新增缺失字段
-- ============================================================

ALTER TABLE `stock_out` ADD COLUMN `approval_instance_id` BIGINT DEFAULT NULL COMMENT '审批实例ID' AFTER `remark`;
ALTER TABLE `stock_out` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_time`;
ALTER TABLE `stock_out` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第25步：stock_out_detail表新增缺失字段
-- ============================================================

ALTER TABLE `stock_out_detail` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `remark`;

-- ============================================================
-- 第26步：stock_transaction表新增缺失字段
-- ============================================================

ALTER TABLE `stock_transaction` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `created_time`;

-- ============================================================
-- 第27步：purchase_suppliers表新增缺失字段
-- ============================================================

ALTER TABLE `purchase_suppliers` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `material_info`;
ALTER TABLE `purchase_suppliers` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;
ALTER TABLE `purchase_suppliers` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;
ALTER TABLE `purchase_suppliers` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第28步：purchase_orders表新增缺失字段
-- ============================================================

ALTER TABLE `purchase_orders` ADD COLUMN `supplier_id` BIGINT DEFAULT NULL COMMENT '供应商ID' AFTER `purchase_number`;
ALTER TABLE `purchase_orders` ADD COLUMN `prod_unit_id` BIGINT DEFAULT NULL COMMENT '仓库（生产单位ID）' AFTER `supplier_id`;
ALTER TABLE `purchase_orders` ADD COLUMN `approval_instance_id` BIGINT DEFAULT NULL COMMENT '审批实例ID' AFTER `status`;
ALTER TABLE `purchase_orders` ADD COLUMN `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID' AFTER `approval_instance_id`;
ALTER TABLE `purchase_orders` ADD COLUMN `updated_by` BIGINT DEFAULT NULL COMMENT '更新人ID' AFTER `created_by`;
ALTER TABLE `purchase_orders` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `updated_by`;
ALTER TABLE `purchase_orders` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;
ALTER TABLE `purchase_orders` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;
ALTER TABLE `purchase_orders` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第29步：purchase_order_items表新增缺失字段
-- ============================================================

ALTER TABLE `purchase_order_items` ADD COLUMN `material_id` BIGINT DEFAULT NULL COMMENT '物料ID' AFTER `sequence_number`;
ALTER TABLE `purchase_order_items` ADD COLUMN `unit_id` BIGINT DEFAULT NULL COMMENT '单位ID' AFTER `dose`;
ALTER TABLE `purchase_order_items` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `difference`;

-- ============================================================
-- 第30步：approval_node表新增缺失字段
-- ============================================================

ALTER TABLE `approval_node` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `reject_to_node_id`;

-- ============================================================
-- 第31步：equipment_maintenance表新增缺失字段
-- ============================================================

ALTER TABLE `equipment_maintenance` ADD COLUMN `maintenance_company` VARCHAR(255) DEFAULT NULL COMMENT '维保公司' AFTER `maintainer`;
ALTER TABLE `equipment_maintenance` ADD COLUMN `contact_phone` VARCHAR(255) DEFAULT NULL COMMENT '联系方式' AFTER `maintenance_company`;

-- ============================================================
-- 恢复原始sql_mode
-- ============================================================

SET sql_mode = @old_sql_mode;

-- ============================================================
-- 修复完成！
-- ============================================================

SET FOREIGN_KEY_CHECKS = 1;
