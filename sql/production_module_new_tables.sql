-- ===================================
-- 新增生产管理模块表
-- ===================================

-- 1. 工单工序执行记录表
CREATE TABLE IF NOT EXISTS `work_order_process_execution` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `work_order_id` BIGINT NOT NULL COMMENT '关联工单ID',
    `process_type_id` BIGINT DEFAULT NULL COMMENT '工序类型ID',
    `room_id` INT DEFAULT NULL COMMENT '配置室ID',
    `equipment_id` INT DEFAULT NULL COMMENT '使用设备ID',
    `step_order` INT DEFAULT NULL COMMENT '工序顺序',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) DEFAULT '' COMMENT '操作人姓名',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `process_qty` DECIMAL(10,3) DEFAULT NULL COMMENT '加工数量',
    `key_process_params` TEXT DEFAULT NULL COMMENT '关键工艺参数',
    `status` VARCHAR(20) DEFAULT '待执行' COMMENT '状态（待执行/执行中/已完成）',
    `remark` VARCHAR(500) DEFAULT '' COMMENT '备注',
    `is_deleted` INT DEFAULT 0 COMMENT '是否已删除',
    `version` INT DEFAULT 1 COMMENT '乐观锁版本号',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `created_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    INDEX `idx_work_order_id` (`work_order_id`),
    INDEX `idx_process_type_id` (`process_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单工序执行记录表';

-- 2. 领料申请表
CREATE TABLE IF NOT EXISTS `material_requisition` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `requisition_code` VARCHAR(50) NOT NULL COMMENT '领料编号',
    `work_order_id` BIGINT DEFAULT NULL COMMENT '关联工单ID',
    `requisition_date` DATE DEFAULT NULL COMMENT '领料日期',
    `warehouse` VARCHAR(50) DEFAULT '' COMMENT '仓库',
    `material_type` VARCHAR(20) DEFAULT '' COMMENT '出库种类（原料/辅料/包材）',
    `multiplier` DECIMAL(10,2) DEFAULT NULL COMMENT '处方倍数',
    `remark` VARCHAR(500) DEFAULT '' COMMENT '备注',
    `status` VARCHAR(20) DEFAULT '草稿' COMMENT '状态（草稿/已提交/已审批/已出库）',
    `is_deleted` INT DEFAULT 0 COMMENT '是否已删除',
    `version` INT DEFAULT 1 COMMENT '乐观锁版本号',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `created_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    UNIQUE KEY `uk_requisition_code` (`requisition_code`),
    INDEX `idx_work_order_id` (`work_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='领料申请表';

-- 3. 领料明细表
CREATE TABLE IF NOT EXISTS `material_requisition_detail` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `requisition_id` BIGINT NOT NULL COMMENT '领料申请ID',
    `material_id` BIGINT DEFAULT NULL COMMENT '物料ID',
    `material_code` VARCHAR(50) DEFAULT '' COMMENT '物料编码',
    `material_name` VARCHAR(100) DEFAULT '' COMMENT '物料名称',
    `material_category` VARCHAR(20) DEFAULT '' COMMENT '分类（原料/辅料/包材）',
    `unit_name` VARCHAR(20) DEFAULT '' COMMENT '单位',
    `prescription_qty` DECIMAL(10,3) DEFAULT NULL COMMENT '处方用量',
    `apply_qty` DECIMAL(10,3) DEFAULT NULL COMMENT '申请数量',
    `stock_qty` DECIMAL(10,3) DEFAULT NULL COMMENT '库存数量',
    `is_deleted` INT DEFAULT 0 COMMENT '是否已删除',
    `version` INT DEFAULT 1 COMMENT '乐观锁版本号',
    INDEX `idx_requisition_id` (`requisition_id`),
    INDEX `idx_material_id` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='领料明细表';
