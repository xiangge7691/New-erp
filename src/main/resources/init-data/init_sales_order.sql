-- =============================================
-- 17-2 成品出库台账表
-- 创建时间：2026-09-21
-- 说明：记录成品出库的记账台账（仅记账），成品出库开单后自动联动出库管理完成成品出库
-- =============================================

CREATE TABLE IF NOT EXISTS `sales_order` (
    `sales_order_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成品出库台账唯一标识',
    `sales_order_code` VARCHAR(30) DEFAULT NULL COMMENT '成品出库单号（唯一，格式CPCK-YYYYMMDD-NNN）',
    `sales_order_date` DATETIME NOT NULL COMMENT '成品出库日期',
    `customer_id` BIGINT NOT NULL COMMENT '客户ID（外键 → customer表）',
    `preparation_name` VARCHAR(200) NOT NULL COMMENT '制剂名称',
    `preparation_code` VARCHAR(50) NOT NULL COMMENT '制剂编码（随制剂自动带出，不可修改）',
    `batch_number` VARCHAR(50) DEFAULT NULL COMMENT '批号（出库时确定）',
    `quantity` INT NOT NULL COMMENT '数量',
    `unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '单价（默认取制剂信息中的价格，可修改）',
    `amount` DECIMAL(12,2) DEFAULT NULL COMMENT '金额（自动计算：数量×单价）',
    `related_out_code` VARCHAR(30) DEFAULT NULL COMMENT '关联系统出库单号（回写）',
    `status` VARCHAR(20) NOT NULL DEFAULT '已开单' COMMENT '状态：已开单/已出库/已作废',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `is_deleted` INT DEFAULT 0 COMMENT '是否已删除（0否1是）',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`sales_order_id`),
    UNIQUE KEY `uk_sales_order_code` (`sales_order_code`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_status` (`status`),
    KEY `idx_sales_order_date` (`sales_order_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成品出库台账表';

-- 插入演示数据
INSERT INTO `sales_order` (`sales_order_code`, `sales_order_date`, `customer_id`, `preparation_name`, `preparation_code`, `quantity`, `unit_price`, `amount`, `status`) VALUES
('CPCK-20260918-001', '2026-09-18 10:30:00', 1, '益肾壮骨丸', 'Z00347', 200, 21.01, 4202.00, '已开单'),
('CPCK-20260918-002', '2026-09-18 11:00:00', 2, '抗骨刺增生合剂', 'Z00348', 300, 12.50, 3750.00, '已出库'),
('CPCK-20260917-001', '2026-09-17 15:00:00', 3, '伸腿瞪眼丸', 'Z00349', 100, 30.00, 3000.00, '已作废');
