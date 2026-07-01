-- ============================================================
-- ERP数据库一键修复脚本（不删库）
-- 基于Entity类与schema_latest.sql的差异分析
-- 执行前请务必备份数据库！
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 第1步：ID字段类型统一为BIGINT
-- ============================================================

-- purchase_suppliers.id: INT → BIGINT
ALTER TABLE `purchase_suppliers` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';

-- purchase_orders.id: INT → BIGINT
ALTER TABLE `purchase_orders` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';

-- purchase_orders.supplier_id: INT → BIGINT
ALTER TABLE `purchase_orders` MODIFY COLUMN `supplier_id` BIGINT DEFAULT NULL COMMENT '供应商ID';

-- purchase_order_items.id: INT → BIGINT
ALTER TABLE `purchase_order_items` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';

-- purchase_order_items.order_id: INT → BIGINT
ALTER TABLE `purchase_order_items` MODIFY COLUMN `order_id` BIGINT NOT NULL COMMENT '关联订单ID';

-- stock_in.supplier_id: INT → BIGINT (如果存在)
-- ALTER TABLE `stock_in` MODIFY COLUMN `supplier_id` BIGINT DEFAULT NULL COMMENT '供应商ID';

-- ============================================================
-- 第2步：时间字段统一命名 created_time/update_time → created_at/updated_at
-- ============================================================

-- user表: created_time → created_at, update_time → updated_at
ALTER TABLE `user` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `user` CHANGE COLUMN `update_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- role表: create_time → created_at, update_time → updated_at
ALTER TABLE `role` CHANGE COLUMN `create_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `role` CHANGE COLUMN `update_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- material表: created_time → created_at, updated_time → updated_at
ALTER TABLE `material` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `material` CHANGE COLUMN `updated_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- preparation表: created_time → created_at, updated_time → updated_at
ALTER TABLE `preparation` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `preparation` CHANGE COLUMN `updated_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- preparation_formula表: created_time → created_at, updated_time → updated_at
ALTER TABLE `preparation_formula` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `preparation_formula` CHANGE COLUMN `updated_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- production_plan表: create_time → created_at, update_time → updated_at
ALTER TABLE `production_plan` CHANGE COLUMN `create_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `production_plan` CHANGE COLUMN `update_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间';

-- production_process_record表: created_time → created_at, updated_time → updated_at
ALTER TABLE `production_process_record` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `production_process_record` CHANGE COLUMN `updated_time` `updated_at` DATETIME DEFAULT NULL COMMENT '最后修改时间';

-- work_order表: created_time → created_at, updated_time → updated_at
ALTER TABLE `work_order` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `work_order` CHANGE COLUMN `updated_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- stock表: created_time → created_at, updated_time → updated_at
ALTER TABLE `stock` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `stock` CHANGE COLUMN `updated_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- stock_in表: created_time → created_at, updated_time → updated_at
ALTER TABLE `stock_in` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `stock_in` CHANGE COLUMN `updated_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- stock_out表: created_time → created_at, updated_time → updated_at
ALTER TABLE `stock_out` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `stock_out` CHANGE COLUMN `updated_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- stock_transaction表: created_time → created_at
ALTER TABLE `stock_transaction` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- file_info表: created_time → created_at, updated_time → updated_at
ALTER TABLE `file_info` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `file_info` CHANGE COLUMN `updated_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- production_unit表: created_time → created_at, update_time → updated_at
ALTER TABLE `production_unit` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `production_unit` CHANGE COLUMN `update_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- prod_unit_invoice表: created_time → created_at, update_time → updated_at
ALTER TABLE `prod_unit_invoice` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `prod_unit_invoice` CHANGE COLUMN `update_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- prod_unit_material_file表: created_time → created_at
ALTER TABLE `prod_unit_material_file` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- room_info表: created_time → created_at, updated_time → updated_at
ALTER TABLE `room_info` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `room_info` CHANGE COLUMN `updated_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间';

-- equipment表: created_time → created_at, updated_time → updated_at
ALTER TABLE `equipment` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `equipment` CHANGE COLUMN `updated_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间';

-- process_type表: created_time → created_at, updated_time → updated_at
ALTER TABLE `process_type` CHANGE COLUMN `created_time` `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `process_type` CHANGE COLUMN `updated_time` `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间';

-- ============================================================
-- 第3步：主键命名统一（user_role/role_perm/user_department → id）
-- ============================================================

-- user_role表: userrole_id → id
ALTER TABLE `user_role` CHANGE COLUMN `userrole_id` `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';

-- role_perm表: roleperm_id → id
ALTER TABLE `role_perm` CHANGE COLUMN `roleperm_id` `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';

-- user_department表: userdepartment_id → id
ALTER TABLE `user_department` CHANGE COLUMN `userdepartment_id` `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';

-- ============================================================
-- 第4步：user表新增缺失字段
-- ============================================================

-- user表: 新增email字段
ALTER TABLE `user` ADD COLUMN `email` VARCHAR(255) DEFAULT NULL COMMENT '邮箱' AFTER `phone`;

-- user表: 新增is_deleted字段
ALTER TABLE `user` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `user_notes`;

-- user表: 新增version字段
ALTER TABLE `user` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第5步：role表新增缺失字段
-- ============================================================

-- role表: 新增is_deleted字段
ALTER TABLE `role` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `role_status`;

-- role表: 新增version字段
ALTER TABLE `role` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第6步：permission表新增缺失字段
-- ============================================================

-- permission表: 新增is_deleted字段
ALTER TABLE `permission` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `perm_status`;

-- permission表: 新增version字段
ALTER TABLE `permission` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第7步：user_role表新增缺失字段
-- ============================================================

-- user_role表: 新增is_deleted字段
ALTER TABLE `user_role` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `created_at`;

-- ============================================================
-- 第8步：role_perm表新增缺失字段
-- ============================================================

-- role_perm表: 新增is_deleted字段
ALTER TABLE `role_perm` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `created_at`;

-- ============================================================
-- 第9步：department表新增缺失字段
-- ============================================================

-- department表: 新增parent_id字段
ALTER TABLE `department` ADD COLUMN `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID（0为顶级）' AFTER `department_name`;

-- department表: 新增status字段
ALTER TABLE `department` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用' AFTER `parent_id`;

-- department表: 新增sort_order字段
ALTER TABLE `department` ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号' AFTER `status`;

-- department表: 新增created_at字段
ALTER TABLE `department` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `sort_order`;

-- department表: 新增updated_at字段
ALTER TABLE `department` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;

-- department表: 新增is_deleted字段
ALTER TABLE `department` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- ============================================================
-- 第10步：user_department表新增缺失字段
-- ============================================================

-- user_department表: 新增is_deleted字段
ALTER TABLE `user_department` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `created_at`;

-- ============================================================
-- 第11步：unit表新增缺失字段
-- ============================================================

-- unit表: 新增status字段
ALTER TABLE `unit` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用' AFTER `symbol`;

-- unit表: 新增created_at字段
ALTER TABLE `unit` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `status`;

-- unit表: 新增updated_at字段
ALTER TABLE `unit` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;

-- unit表: 新增is_deleted字段
ALTER TABLE `unit` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- ============================================================
-- 第12步：dosage_form表新增缺失字段
-- ============================================================

-- dosage_form表: 新增status字段
ALTER TABLE `dosage_form` ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用' AFTER `remark`;

-- dosage_form表: 新增created_at字段
ALTER TABLE `dosage_form` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `status`;

-- dosage_form表: 新增updated_at字段
ALTER TABLE `dosage_form` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;

-- dosage_form表: 新增is_deleted字段
ALTER TABLE `dosage_form` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- ============================================================
-- 第13步：material表新增缺失字段
-- ============================================================

-- material表: 新增is_deleted字段
ALTER TABLE `material` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- material表: 新增version字段
ALTER TABLE `material` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第14步：room_info表新增缺失字段
-- ============================================================

-- room_info表: 新增production_type字段
ALTER TABLE `room_info` ADD COLUMN `production_type` VARCHAR(255) DEFAULT NULL COMMENT '生产制剂类型' AFTER `area`;

-- room_info表: 新增dust_room字段
ALTER TABLE `room_info` ADD COLUMN `dust_room` TINYINT(1) DEFAULT 0 COMMENT '是否为产尘操作间' AFTER `production_type`;

-- room_info表: 新增clean_area字段
ALTER TABLE `room_info` ADD COLUMN `clean_area` TINYINT(1) DEFAULT 0 COMMENT '是否为洁净区' AFTER `dust_room`;

-- room_info表: 新增clean_grade字段
ALTER TABLE `room_info` ADD COLUMN `clean_grade` VARCHAR(50) DEFAULT NULL COMMENT '洁净等级' AFTER `clean_area`;

-- room_info表: 新增clean_procedure_file_id字段
ALTER TABLE `room_info` ADD COLUMN `clean_procedure_file_id` BIGINT DEFAULT NULL COMMENT '洁净规程文件ID' AFTER `clean_grade`;

-- room_info表: 新增is_deleted字段
ALTER TABLE `room_info` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- room_info表: 新增version字段
ALTER TABLE `room_info` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第15步：equipment表新增缺失字段
-- ============================================================

-- equipment表: 新增equipment_type字段
ALTER TABLE `equipment` ADD COLUMN `equipment_type` VARCHAR(50) DEFAULT NULL COMMENT '设备类型' AFTER `last_maintenance_date`;

-- equipment表: 新增maintenance_cycle字段
ALTER TABLE `equipment` ADD COLUMN `maintenance_cycle` INT DEFAULT 6 COMMENT '维保周期（月）' AFTER `equipment_type`;

-- equipment表: 新增reminder_days字段
ALTER TABLE `equipment` ADD COLUMN `reminder_days` INT DEFAULT 15 COMMENT '到期提醒天数' AFTER `maintenance_cycle`;

-- equipment表: 新增next_maintenance_date字段
ALTER TABLE `equipment` ADD COLUMN `next_maintenance_date` DATE DEFAULT NULL COMMENT '下次维保时间' AFTER `reminder_days`;

-- equipment表: 新增is_deleted字段
ALTER TABLE `equipment` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- equipment表: 新增version字段
ALTER TABLE `equipment` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第16步：process_type表新增缺失字段
-- ============================================================

-- process_type表: 新增is_deleted字段
ALTER TABLE `process_type` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- process_type表: 新增version字段
ALTER TABLE `process_type` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第17步：production_unit表新增缺失字段
-- ============================================================

-- production_unit表: 新增is_deleted字段
ALTER TABLE `production_unit` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- production_unit表: 新增version字段
ALTER TABLE `production_unit` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第18步：prod_unit_invoice表新增缺失字段
-- ============================================================

-- prod_unit_invoice表: 新增is_deleted字段
ALTER TABLE `prod_unit_invoice` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- ============================================================
-- 第19步：prod_unit_material_file表新增缺失字段
-- ============================================================

-- prod_unit_material_file表: 新增file_path字段
ALTER TABLE `prod_unit_material_file` ADD COLUMN `file_path` VARCHAR(500) DEFAULT NULL COMMENT '文件存储路径' AFTER `file_size`;

-- prod_unit_material_file表: 新增is_deleted字段
ALTER TABLE `prod_unit_material_file` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `created_at`;

-- ============================================================
-- 第20步：preparation表新增缺失字段
-- ============================================================

-- preparation表: 新增is_deleted字段
ALTER TABLE `preparation` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- preparation表: 新增version字段
ALTER TABLE `preparation` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第21步：preparation_formula表新增缺失字段
-- ============================================================

-- preparation_formula表: 新增is_deleted字段
ALTER TABLE `preparation_formula` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- ============================================================
-- 第22步：production_plan表新增缺失字段
-- ============================================================

-- production_plan表: 新增is_deleted字段
ALTER TABLE `production_plan` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- production_plan表: 新增version字段
ALTER TABLE `production_plan` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第23步：work_order表新增缺失字段
-- ============================================================

-- work_order表: 新增is_deleted字段
ALTER TABLE `work_order` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- work_order表: 新增version字段
ALTER TABLE `work_order` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第24步：stock表新增缺失字段
-- ============================================================

-- stock表: 新增category_id字段（外键替代category_name）
ALTER TABLE `stock` ADD COLUMN `category_id` BIGINT DEFAULT NULL COMMENT '分类ID' AFTER `item_id`;

-- stock表: 新增unit_id字段（外键替代unit_name）
ALTER TABLE `stock` ADD COLUMN `unit_id` BIGINT DEFAULT NULL COMMENT '计量单位ID' AFTER `category_id`;

-- stock表: 新增is_deleted字段
ALTER TABLE `stock` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- stock表: 新增version字段
ALTER TABLE `stock` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第25步：stock_in表新增缺失字段
-- ============================================================

-- stock_in表: 新增approval_instance_id字段
ALTER TABLE `stock_in` ADD COLUMN `approval_instance_id` BIGINT DEFAULT NULL COMMENT '审批实例ID' AFTER `in_status`;

-- stock_in表: 新增is_deleted字段
ALTER TABLE `stock_in` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- stock_in表: 新增version字段
ALTER TABLE `stock_in` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第26步：stock_in_detail表新增缺失字段
-- ============================================================

-- stock_in_detail表: 新增category_id字段（外键替代category_name）
ALTER TABLE `stock_in_detail` ADD COLUMN `category_id` BIGINT DEFAULT NULL COMMENT '分类ID' AFTER `item_id`;

-- stock_in_detail表: 新增unit_id字段（外键替代unit_name）
ALTER TABLE `stock_in_detail` ADD COLUMN `unit_id` BIGINT DEFAULT NULL COMMENT '单位ID' AFTER `category_id`;

-- stock_in_detail表: 新增is_deleted字段
ALTER TABLE `stock_in_detail` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `remark`;

-- ============================================================
-- 第27步：stock_out表新增缺失字段
-- ============================================================

-- stock_out表: 新增approval_instance_id字段
ALTER TABLE `stock_out` ADD COLUMN `approval_instance_id` BIGINT DEFAULT NULL COMMENT '审批实例ID' AFTER `out_status`;

-- stock_out表: 新增is_deleted字段
ALTER TABLE `stock_out` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- stock_out表: 新增version字段
ALTER TABLE `stock_out` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第28步：stock_out_detail表新增缺失字段
-- ============================================================

-- stock_out_detail表: 新增category_id字段（外键替代category_name）
ALTER TABLE `stock_out_detail` ADD COLUMN `category_id` BIGINT DEFAULT NULL COMMENT '分类ID' AFTER `item_id`;

-- stock_out_detail表: 新增unit_id字段（外键替代unit_name）
ALTER TABLE `stock_out_detail` ADD COLUMN `unit_id` BIGINT DEFAULT NULL COMMENT '单位ID' AFTER `category_id`;

-- stock_out_detail表: 新增is_deleted字段
ALTER TABLE `stock_out_detail` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `remark`;

-- ============================================================
-- 第29步：stock_transaction表新增缺失字段
-- ============================================================

-- stock_transaction表: 新增is_deleted字段
ALTER TABLE `stock_transaction` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `created_at`;

-- ============================================================
-- 第30步：purchase_suppliers表新增缺失字段
-- ============================================================

-- purchase_suppliers表: 新增created_at字段
ALTER TABLE `purchase_suppliers` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `material_info`;

-- purchase_suppliers表: 新增updated_at字段
ALTER TABLE `purchase_suppliers` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;

-- purchase_suppliers表: 新增is_deleted字段
ALTER TABLE `purchase_suppliers` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- purchase_suppliers表: 新增version字段
ALTER TABLE `purchase_suppliers` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第31步：purchase_orders表新增缺失字段
-- ============================================================

-- purchase_orders表: 新增supplier_id字段（如果不存在）
-- ALTER TABLE `purchase_orders` ADD COLUMN `supplier_id` BIGINT DEFAULT NULL COMMENT '供应商ID' AFTER `purchase_number`;

-- purchase_orders表: 新增prod_unit_id字段
ALTER TABLE `purchase_orders` ADD COLUMN `prod_unit_id` BIGINT DEFAULT NULL COMMENT '仓库（生产单位ID）' AFTER `supplier_id`;

-- purchase_orders表: 新增approval_instance_id字段
ALTER TABLE `purchase_orders` ADD COLUMN `approval_instance_id` BIGINT DEFAULT NULL COMMENT '审批实例ID' AFTER `status`;

-- purchase_orders表: 新增created_by字段
ALTER TABLE `purchase_orders` ADD COLUMN `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID' AFTER `approval_instance_id`;

-- purchase_orders表: 新增updated_by字段
ALTER TABLE `purchase_orders` ADD COLUMN `updated_by` BIGINT DEFAULT NULL COMMENT '更新人ID' AFTER `created_by`;

-- purchase_orders表: 新增created_at字段
ALTER TABLE `purchase_orders` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `updated_by`;

-- purchase_orders表: 新增updated_at字段
ALTER TABLE `purchase_orders` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_at`;

-- purchase_orders表: 新增is_deleted字段
ALTER TABLE `purchase_orders` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- purchase_orders表: 新增version字段
ALTER TABLE `purchase_orders` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;

-- ============================================================
-- 第32步：purchase_order_items表新增缺失字段
-- ============================================================

-- purchase_order_items表: 新增material_id字段（外键替代raw_material_name）
ALTER TABLE `purchase_order_items` ADD COLUMN `material_id` BIGINT DEFAULT NULL COMMENT '物料ID' AFTER `sequence_number`;

-- purchase_order_items表: 新增unit_id字段（外键替代unit）
ALTER TABLE `purchase_order_items` ADD COLUMN `unit_id` BIGINT DEFAULT NULL COMMENT '单位ID' AFTER `dose`;

-- purchase_order_items表: 新增is_deleted字段
ALTER TABLE `purchase_order_items` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `difference`;

-- ============================================================
-- 第33步：approval_node表新增缺失字段
-- ============================================================

-- approval_node表: 新增is_deleted字段
ALTER TABLE `approval_node` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `updated_at`;

-- ============================================================
-- 第34步：approval_record表新增缺失字段
-- ============================================================

-- approval_record表: 新增is_deleted字段
ALTER TABLE `approval_record` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `created_at`;

-- ============================================================
-- 第35步：创建缺失的表（环境监测模块）
-- ============================================================

CREATE TABLE IF NOT EXISTS `pressure_difference_record` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `room_id`         BIGINT                DEFAULT NULL COMMENT '房间ID',
    `record_date`     DATE                  DEFAULT NULL COMMENT '记录日期',
    `inspection_area` VARCHAR(255)          DEFAULT NULL COMMENT '检测区域',
    `pressure_value`  DECIMAL(10,2)         DEFAULT NULL COMMENT '压差值',
    `recorder`        VARCHAR(255)          DEFAULT NULL COMMENT '记录人',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    CONSTRAINT `fk_pdr_room` FOREIGN KEY (`room_id`) REFERENCES `room_info` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='压差记录表';

CREATE TABLE IF NOT EXISTS `temperature_humidity_record` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `room_id`         BIGINT                DEFAULT NULL COMMENT '房间ID',
    `record_date`     DATE                  DEFAULT NULL COMMENT '记录日期',
    `inspection_area` VARCHAR(255)          DEFAULT NULL COMMENT '检测区域',
    `temperature`     DECIMAL(5,2)          DEFAULT NULL COMMENT '温度',
    `humidity`        DECIMAL(5,2)          DEFAULT NULL COMMENT '湿度',
    `recorder`        VARCHAR(255)          DEFAULT NULL COMMENT '记录人',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    CONSTRAINT `fk_thr_room` FOREIGN KEY (`room_id`) REFERENCES `room_info` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='温湿度记录表';

CREATE TABLE IF NOT EXISTS `clean_inspection_record` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `room_id`          BIGINT                DEFAULT NULL COMMENT '房间ID',
    `inspection_date`  DATE                  DEFAULT NULL COMMENT '检测日期',
    `inspection_area`  VARCHAR(255)          DEFAULT NULL COMMENT '检测区域',
    `inspection_item`  VARCHAR(255)          DEFAULT NULL COMMENT '检测项目',
    `inspection_result` VARCHAR(255)         DEFAULT NULL COMMENT '检测结果',
    `inspector`        VARCHAR(255)          DEFAULT NULL COMMENT '检测人',
    `report_file_id`   BIGINT                DEFAULT NULL COMMENT '报告文件ID',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    CONSTRAINT `fk_cir_room` FOREIGN KEY (`room_id`) REFERENCES `room_info` (`room_id`),
    CONSTRAINT `fk_cir_file` FOREIGN KEY (`report_file_id`) REFERENCES `file_info` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='洁净检测记录表';

CREATE TABLE IF NOT EXISTS `disinfection_record` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `room_id`              BIGINT                DEFAULT NULL COMMENT '房间ID',
    `disinfection_date`    DATE                  DEFAULT NULL COMMENT '消毒日期',
    `disinfection_method`  VARCHAR(255)          DEFAULT NULL COMMENT '消毒方式',
    `disinfection_person`  VARCHAR(255)          DEFAULT NULL COMMENT '消毒人员',
    `disinfection_cycle`   INT                   DEFAULT NULL COMMENT '消毒周期（天）',
    `next_disinfection_date` DATE                DEFAULT NULL COMMENT '下次消毒日期',
    `remark`               VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`           TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    CONSTRAINT `fk_dr_room` FOREIGN KEY (`room_id`) REFERENCES `room_info` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消毒管理记录表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 修复完成！
-- 请检查应用日志确认无报错
-- ============================================================
