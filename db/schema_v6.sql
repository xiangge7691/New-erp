-- ============================================================
-- ERP系统数据库DDL脚本 v6（设备管理功能补全）
-- 基于v5，补充设备维保设置、维保记录扩展字段
-- 数据库: MySQL 8.0+  引擎: InnoDB  字符集: utf8mb4
-- ============================================================

-- ============================================================
-- 1. equipment表 - 新增维保设置字段
-- ============================================================

ALTER TABLE `equipment`
    ADD COLUMN `equipment_type` VARCHAR(50) DEFAULT NULL COMMENT '设备类型：生产设备/检验设备/环境设备/其他设备' AFTER `equipment_model`,
    ADD COLUMN `maintenance_cycle` INT DEFAULT 6 COMMENT '维保周期（月），默认6个月' AFTER `last_maintenance_date`,
    ADD COLUMN `reminder_days` INT DEFAULT 15 COMMENT '到期提醒天数，默认15天' AFTER `maintenance_cycle`,
    ADD COLUMN `next_maintenance_date` DATE DEFAULT NULL COMMENT '下次维保时间' AFTER `reminder_days`;

-- ============================================================
-- 2. equipment_maintenance表 - 新增扩展字段
-- ============================================================

ALTER TABLE `equipment_maintenance`
    ADD COLUMN `maintenance_company` VARCHAR(100) DEFAULT NULL COMMENT '维保公司' AFTER `maintainer`,
    ADD COLUMN `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系方式' AFTER `maintenance_company`,
    ADD COLUMN `next_maintenance_content` TEXT DEFAULT NULL COMMENT '下次维保内容' AFTER `next_maintenance_date`,
    ADD COLUMN `attachment` VARCHAR(500) DEFAULT NULL COMMENT '附件路径' AFTER `next_maintenance_content`;
