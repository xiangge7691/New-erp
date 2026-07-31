-- ===================================
-- 采购管理模块 - 新建表和修改表
-- ===================================

-- 1. 创建采购计划表
CREATE TABLE IF NOT EXISTS `purchase_plan` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `plan_code` VARCHAR(50) NOT NULL COMMENT '采购计划编号',
    `work_order_id` BIGINT DEFAULT NULL COMMENT '关联生产任务ID',
    `work_order_code` VARCHAR(50) DEFAULT '' COMMENT '生产任务编号',
    `title` VARCHAR(200) DEFAULT '' COMMENT '工单标题',
    `preparation_code` VARCHAR(50) DEFAULT '' COMMENT '制剂编码',
    `preparation_name` VARCHAR(200) DEFAULT '' COMMENT '制剂名称',
    `spec` VARCHAR(200) DEFAULT '' COMMENT '规格',
    `batch_qty` DECIMAL(10,3) DEFAULT NULL COMMENT '批量',
    `prescription_multiple` DECIMAL(10,2) DEFAULT NULL COMMENT '处方倍数',
    `material_type` VARCHAR(20) DEFAULT '' COMMENT '物料类型',
    `warehouse` VARCHAR(50) DEFAULT '' COMMENT '仓库',
    `processing_date` DATE DEFAULT NULL COMMENT '处理日期',
    `expected_delivery_date` DATE DEFAULT NULL COMMENT '预计到货日期',
    `receiving_unit` VARCHAR(100) DEFAULT '' COMMENT '收货单位',
    `receiving_address` VARCHAR(200) DEFAULT '' COMMENT '收货地址',
    `invoice_info` VARCHAR(200) DEFAULT '' COMMENT '发票信息',
    `remark` VARCHAR(500) DEFAULT '' COMMENT '备注',
    `status` VARCHAR(20) DEFAULT '草稿' COMMENT '状态（草稿/待审批/已审批/已驳回）',
    `approval_opinion` VARCHAR(500) DEFAULT '' COMMENT '审批意见',
    `purchase_order_id` BIGINT DEFAULT NULL COMMENT '关联的采购订单ID',
    `is_deleted` INT DEFAULT 0 COMMENT '是否已删除',
    `version` INT DEFAULT 1 COMMENT '乐观锁版本号',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `created_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    UNIQUE KEY `uk_plan_code` (`plan_code`),
    INDEX `idx_work_order_id` (`work_order_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购计划表';

-- 2. 创建采购计划明细表
CREATE TABLE IF NOT EXISTS `purchase_plan_detail` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `plan_id` BIGINT NOT NULL COMMENT '采购计划ID',
    `sequence_number` INT DEFAULT NULL COMMENT '序号',
    `material_id` BIGINT DEFAULT NULL COMMENT '物料ID',
    `material_code` VARCHAR(50) DEFAULT '' COMMENT '原料编码',
    `material_name` VARCHAR(100) DEFAULT '' COMMENT '原料名称',
    `material_category` VARCHAR(20) DEFAULT '' COMMENT '原料分类',
    `unit` VARCHAR(20) DEFAULT '' COMMENT '单位',
    `standard_qty` DECIMAL(10,3) DEFAULT NULL COMMENT '标准处方量',
    `purchase_qty` DECIMAL(10,3) DEFAULT NULL COMMENT '采购数量',
    `stock_qty` DECIMAL(10,3) DEFAULT NULL COMMENT '库存数量',
    `difference` DECIMAL(10,3) DEFAULT NULL COMMENT '标准量差值',
    `is_deleted` INT DEFAULT 0 COMMENT '是否已删除',
    `version` INT DEFAULT 1 COMMENT '乐观锁版本号',
    INDEX `idx_plan_id` (`plan_id`),
    INDEX `idx_material_id` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购计划明细表';

-- 3. 采购订单表新增字段
ALTER TABLE `purchase_orders`
ADD COLUMN `plan_id` BIGINT DEFAULT NULL COMMENT '关联采购计划ID' AFTER `title`,
ADD COLUMN `plan_code` VARCHAR(50) DEFAULT '' COMMENT '采购计划编号' AFTER `plan_id`,
ADD COLUMN `work_order_id` BIGINT DEFAULT NULL COMMENT '关联生产任务ID' AFTER `plan_code`,
ADD COLUMN `work_order_code` VARCHAR(50) DEFAULT '' COMMENT '生产任务编号' AFTER `work_order_id`,
ADD COLUMN `preparation_code` VARCHAR(50) DEFAULT '' COMMENT '制剂编码' AFTER `work_order_code`,
ADD COLUMN `preparation_name` VARCHAR(200) DEFAULT '' COMMENT '制剂名称' AFTER `preparation_code`,
ADD COLUMN `spec` VARCHAR(200) DEFAULT '' COMMENT '规格' AFTER `preparation_name`,
ADD COLUMN `batch_qty` DECIMAL(10,3) DEFAULT NULL COMMENT '批量' AFTER `spec`,
ADD COLUMN `material_type` VARCHAR(20) DEFAULT '' COMMENT '物料类型' AFTER `prescription_multiple`,
ADD COLUMN `approval_opinion` VARCHAR(500) DEFAULT '' COMMENT '审批意见' AFTER `status`;
