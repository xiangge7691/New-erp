-- ============================================================
-- 一次性修复所有缺少的is_deleted和version列
-- 如果列已存在会报错但不影响执行
-- ============================================================

SET @sql = '';

-- user表
SET @sql = CONCAT(@sql, 'ALTER TABLE `user` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- department表
SET @sql = CONCAT(@sql, 'ALTER TABLE `department` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- user_role表
SET @sql = CONCAT(@sql, 'ALTER TABLE `user_role` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- user_department表
SET @sql = CONCAT(@sql, 'ALTER TABLE `user_department` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- unit表
SET @sql = CONCAT(@sql, 'ALTER TABLE `unit` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- prod_unit_invoice表
SET @sql = CONCAT(@sql, 'ALTER TABLE `prod_unit_invoice` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- stock_in_detail表
SET @sql = CONCAT(@sql, 'ALTER TABLE `stock_in_detail` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- stock_out_detail表
SET @sql = CONCAT(@sql, 'ALTER TABLE `stock_out_detail` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- stock_transaction表
SET @sql = CONCAT(@sql, 'ALTER TABLE `stock_transaction` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- approval_workflow表
SET @sql = CONCAT(@sql, 'ALTER TABLE `approval_workflow` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- approval_node表
SET @sql = CONCAT(@sql, 'ALTER TABLE `approval_node` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- approval_instance表
SET @sql = CONCAT(@sql, 'ALTER TABLE `approval_instance` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- approval_record表
SET @sql = CONCAT(@sql, 'ALTER TABLE `approval_record` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- preparation_formula表
SET @sql = CONCAT(@sql, 'ALTER TABLE `preparation_formula` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- file_info表
SET @sql = CONCAT(@sql, 'ALTER TABLE `file_info` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否已删除'' AFTER `updated_time`;');
SET @sql = CONCAT(@sql, 'ALTER TABLE `file_info` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- business_file表
SET @sql = CONCAT(@sql, 'ALTER TABLE `business_file` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否已删除'' AFTER `created_at`;');
SET @sql = CONCAT(@sql, 'ALTER TABLE `business_file` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

-- operation_log表
SET @sql = CONCAT(@sql, 'ALTER TABLE `operation_log` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否已删除'' AFTER `created_at`;');
SET @sql = CONCAT(@sql, 'ALTER TABLE `operation_log` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'' AFTER `is_deleted`;');

SELECT @sql;
