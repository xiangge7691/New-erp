-- ===================================
-- 培训管理模块与供应商审核模块建表SQL
-- ===================================

-- 1. 培训记录表
CREATE TABLE IF NOT EXISTS `training_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `training_no` VARCHAR(50) NOT NULL COMMENT '培训编号（TRAIN-年-序号）',
    `training_name` VARCHAR(200) NOT NULL COMMENT '培训名称',
    `training_category` VARCHAR(50) NOT NULL COMMENT '培训类别（岗前培训/在岗培训/专项培训/继续教育）',
    `training_form` VARCHAR(50) NOT NULL COMMENT '培训形式（内部授课/外部培训/线上学习/实操演练）',
    `training_content` TEXT COMMENT '培训内容',
    `training_date` DATE NOT NULL COMMENT '培训日期',
    `training_duration` INT COMMENT '培训时长（小时）',
    `training_location` VARCHAR(100) COMMENT '培训地点',
    `trainer` VARCHAR(50) COMMENT '培训讲师',
    `training_unit` VARCHAR(100) COMMENT '培训单位',
    `training_cycle` INT COMMENT '培训周期（月）',
    `next_training_date` DATE COMMENT '下次培训日期',
    `remark` VARCHAR(500) COMMENT '备注',
    `is_deleted` INT DEFAULT 0 COMMENT '是否已删除（0否1是）',
    `version` INT DEFAULT 1 COMMENT '乐观锁版本号',
    `created_by` BIGINT COMMENT '创建人ID',
    `updated_by` BIGINT COMMENT '更新人ID',
    `created_time` DATETIME COMMENT '创建时间',
    `updated_time` DATETIME COMMENT '更新时间',
    UNIQUE KEY `uk_training_no` (`training_no`),
    INDEX `idx_training_category` (`training_category`),
    INDEX `idx_training_form` (`training_form`),
    INDEX `idx_training_date` (`training_date`),
    INDEX `idx_next_training_date` (`next_training_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='培训记录表';

-- 2. 供应商审核记录表
CREATE TABLE IF NOT EXISTS `supplier_audit` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `supplier_id` BIGINT NOT NULL COMMENT '关联供应商ID',
    `supplier_name` VARCHAR(200) NOT NULL COMMENT '供应商名称',
    `supply_type` VARCHAR(50) COMMENT '供应类型（原料/辅料/包材/其他）',
    `audit_date` DATE NOT NULL COMMENT '审核日期',
    `audit_content` TEXT COMMENT '审核内容',
    `audit_result` VARCHAR(20) NOT NULL COMMENT '审核结果（合格/基本合格/不合格）',
    `auditor` VARCHAR(50) COMMENT '审核人',
    `audit_cycle` INT DEFAULT 12 COMMENT '审核周期（月）',
    `next_audit_date` DATE COMMENT '下次审核日期',
    `remark` VARCHAR(500) COMMENT '备注',
    `is_deleted` INT DEFAULT 0 COMMENT '是否已删除（0否1是）',
    `version` INT DEFAULT 1 COMMENT '乐观锁版本号',
    `created_by` BIGINT COMMENT '创建人ID',
    `updated_by` BIGINT COMMENT '更新人ID',
    `created_time` DATETIME COMMENT '创建时间',
    `updated_time` DATETIME COMMENT '更新时间',
    INDEX `idx_supplier_id` (`supplier_id`),
    INDEX `idx_supply_type` (`supply_type`),
    INDEX `idx_audit_date` (`audit_date`),
    INDEX `idx_audit_result` (`audit_result`),
    INDEX `idx_next_audit_date` (`next_audit_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='供应商审核记录表';

-- 3. 修改现有供应商表的category字段注释
ALTER TABLE `purchase_suppliers`
MODIFY COLUMN `category` VARCHAR(50) COMMENT '供应类型（原料/辅料/包材/其他）';

-- 4. 房间信息表添加唯一编码字段
ALTER TABLE `room_info`
ADD COLUMN `room_code` VARCHAR(50) DEFAULT NULL COMMENT '房间编码（唯一）' AFTER `room_id`;

-- 添加唯一索引
ALTER TABLE `room_info`
ADD UNIQUE INDEX `uk_room_code` (`room_code`);
