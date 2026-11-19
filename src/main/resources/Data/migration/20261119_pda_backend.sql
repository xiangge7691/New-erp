-- ===================================
-- PDA 移动作业后端支持
-- 测试库：192.168.5.100 / erp_db
-- 日期：2026-11-19
-- ===================================

-- 一、生产工序执行 PDA 字段
ALTER TABLE `work_order_process_execution`
  ADD COLUMN `pda_submitted` TINYINT NOT NULL DEFAULT 0 COMMENT 'PDA是否已提交：0否/1是',
  ADD COLUMN `pda_submit_by` BIGINT NULL COMMENT 'PDA提交人ID',
  ADD COLUMN `pda_submit_time` DATETIME NULL COMMENT 'PDA提交时间',
  ADD COLUMN `pda_device_no` VARCHAR(64) NULL COMMENT 'PDA设备编号',
  ADD COLUMN `manual_time` TINYINT NOT NULL DEFAULT 0 COMMENT '是否手填时间：0否/1是',
  ADD COLUMN `manual_reason` VARCHAR(200) NULL COMMENT '手填时间原因',
  ADD COLUMN `client_request_id` VARCHAR(64) NULL COMMENT '客户端幂等ID';

ALTER TABLE `work_order_process_execution`
  ADD UNIQUE KEY `uk_execution_client_request` (`client_request_id`),
  ADD KEY `idx_execution_equipment_time` (`equipment_id`, `start_time`, `end_time`),
  ADD KEY `idx_execution_pda_submitted` (`pda_submitted`);

-- 二、仓储逐条入库标签与扫码
CREATE TABLE IF NOT EXISTS `stock_in_label` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `in_id` BIGINT UNSIGNED NOT NULL COMMENT '入库单ID',
  `in_detail_id` BIGINT UNSIGNED NOT NULL COMMENT '入库明细ID',
  `label_code` VARCHAR(64) NOT NULL COMMENT '逐条入库码（唯一）',
  `qr_content` VARCHAR(128) NOT NULL COMMENT '二维码内容',
  `print_status` VARCHAR(20) NOT NULL DEFAULT '待打印' COMMENT '打印状态：待打印/已打印',
  `printed_by` BIGINT NULL COMMENT '打印人ID',
  `printed_time` DATETIME NULL COMMENT '打印时间',
  `reprint_count` INT NOT NULL DEFAULT 0 COMMENT '补打次数',
  `scan_status` VARCHAR(20) NOT NULL DEFAULT '待扫' COMMENT '扫码状态：待扫/已扫/作废',
  `scanned_by` BIGINT NULL COMMENT '扫码人ID',
  `scanned_time` DATETIME NULL COMMENT '扫码时间',
  `created_by` BIGINT NULL COMMENT '创建人',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_in_label_code` (`label_code`),
  UNIQUE KEY `uk_stock_in_label_detail` (`in_detail_id`),
  KEY `idx_stock_in_label_in` (`in_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库明细逐条标签';

CREATE TABLE IF NOT EXISTS `stock_scan_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_type` VARCHAR(20) NOT NULL COMMENT '业务类型：INBOUND/OUTBOUND/RETURN',
  `biz_id` BIGINT NOT NULL COMMENT '业务单据ID',
  `detail_id` BIGINT NOT NULL COMMENT '明细ID',
  `label_code` VARCHAR(64) NOT NULL COMMENT '扫描标签码',
  `action` VARCHAR(20) NOT NULL DEFAULT 'SCAN' COMMENT '动作：SCAN/CLEAR/MANUAL',
  `result` VARCHAR(20) NOT NULL COMMENT '结果：SUCCESS/FAIL',
  `fail_reason` VARCHAR(200) NULL COMMENT '失败原因',
  `operator_id` BIGINT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(50) NULL COMMENT '操作人姓名',
  `device_no` VARCHAR(64) NULL COMMENT 'PDA设备编号',
  `request_id` VARCHAR(64) NULL COMMENT '客户端幂等ID',
  `remark` VARCHAR(500) NULL COMMENT '备注：手工录入授权/原因',
  `scan_time` DATETIME NOT NULL COMMENT '扫码时间',
  `created_by` BIGINT NULL COMMENT '创建人',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_scan_request` (`request_id`),
  KEY `idx_stock_scan_biz` (`biz_type`, `biz_id`),
  KEY `idx_stock_scan_label` (`label_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓储扫码记录';

CREATE TABLE IF NOT EXISTS `stock_label_print_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `in_id` BIGINT UNSIGNED NOT NULL COMMENT '入库单ID',
  `in_detail_id` BIGINT NULL COMMENT '入库明细ID',
  `label_code` VARCHAR(64) NULL COMMENT '标签码',
  `action` VARCHAR(20) NOT NULL COMMENT '动作：PRINT/REPRINT',
  `reason` VARCHAR(200) NULL COMMENT '补打原因',
  `operator_id` BIGINT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(50) NULL COMMENT '操作人姓名',
  `device_no` VARCHAR(64) NULL COMMENT 'PDA设备编号',
  `created_by` BIGINT NULL COMMENT '创建人',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  KEY `idx_label_print_in` (`in_id`),
  KEY `idx_label_print_code` (`label_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库标签打印/补打记录';

ALTER TABLE `stock_in`
  ADD COLUMN `scan_status` VARCHAR(20) NOT NULL DEFAULT '已入库' COMMENT 'PDA扫码状态：待打印/待扫码/已入库',
  ADD COLUMN `confirm_by` BIGINT NULL COMMENT '确认人ID',
  ADD COLUMN `confirm_time` DATETIME NULL COMMENT '确认时间',
  ADD COLUMN `confirm_device_no` VARCHAR(64) NULL COMMENT '确认设备编号';

ALTER TABLE `stock_out`
  ADD COLUMN `scan_status` VARCHAR(20) NOT NULL DEFAULT '已扫码' COMMENT 'PDA扫码状态：待扫码/已出库',
  ADD COLUMN `confirm_by` BIGINT NULL COMMENT '确认人ID',
  ADD COLUMN `confirm_time` DATETIME NULL COMMENT '确认时间',
  ADD COLUMN `confirm_device_no` VARCHAR(64) NULL COMMENT '确认设备编号';

ALTER TABLE `stock_out_detail`
  ADD COLUMN `expected_label_code` VARCHAR(64) NULL COMMENT '出库应扫标签码快照';

ALTER TABLE `return_order`
  ADD COLUMN `scan_status` VARCHAR(20) NOT NULL DEFAULT '已确认' COMMENT 'PDA扫码状态：待扫码/已确认',
  ADD COLUMN `confirm_by` BIGINT NULL COMMENT '确认人ID',
  ADD COLUMN `confirm_time` DATETIME NULL COMMENT '确认时间',
  ADD COLUMN `confirm_device_no` VARCHAR(64) NULL COMMENT '确认设备编号';

ALTER TABLE `return_order_detail`
  ADD COLUMN `expected_label_code` VARCHAR(64) NULL COMMENT '退库应扫标签码快照';

-- 三、PDA 设备登记（暂时只建表，登录适配时使用）
CREATE TABLE IF NOT EXISTS `pda_device` (
  `device_no` VARCHAR(64) NOT NULL COMMENT '设备编号',
  `device_name` VARCHAR(100) NULL COMMENT '设备名称',
  `device_model` VARCHAR(100) NULL COMMENT '设备型号',
  `device_status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用/1启用',
  `bound_user_id` BIGINT NULL COMMENT '绑定用户ID',
  `bound_time` DATETIME NULL COMMENT '绑定时间',
  `last_login_time` DATETIME NULL COMMENT '最近登录时间',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT NULL COMMENT '创建人',
  `updated_by` BIGINT NULL COMMENT '更新人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  PRIMARY KEY (`device_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PDA设备登记';
