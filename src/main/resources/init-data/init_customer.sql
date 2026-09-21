-- =============================================
-- 17-1 客户信息表
-- 创建时间：2026-09-21
-- 说明：维护制剂成品客户的基本信息（本院/外院），作为成品销售台账的关联对象来源
-- =============================================

CREATE TABLE IF NOT EXISTS `customer` (
    `customer_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客户唯一标识',
    `customer_code` VARCHAR(10) DEFAULT NULL COMMENT '客户编号（4位数字递增，如0001）',
    `customer_name` VARCHAR(200) NOT NULL COMMENT '客户名称',
    `type` VARCHAR(20) DEFAULT NULL COMMENT '类型：本院/外院（可为空）',
    `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    `phone` VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
    `address` VARCHAR(500) DEFAULT NULL COMMENT '收货/联系地址',
    `invoice_info` TEXT DEFAULT NULL COMMENT '开票信息（发票抬头/税号等）',
    `status` VARCHAR(10) NOT NULL DEFAULT '启用' COMMENT '状态：启用/停用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `is_deleted` INT DEFAULT 0 COMMENT '是否已删除（0否1是）',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`customer_id`),
    UNIQUE KEY `uk_customer_code` (`customer_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户信息表';

-- 插入演示数据
INSERT INTO `customer` (`customer_code`, `customer_name`, `type`, `contact_person`, `phone`, `address`, `invoice_info`, `status`, `remark`) VALUES
('0001', '邵阳市中医医院', '本院', '李主任', '0739-8888888', '邵阳市双清区东大路631号', '邵阳市中医医院；税号91430500123456789X', '启用', NULL),
('0002', '株洲市中医伤科医院', '外院', '王院长', '0733-6666666', '株洲市芦淞区解放街', '株洲市中医伤科医院；税号91430200123456789Y', '启用', NULL),
('0003', '耒阳仁和大药房', '外院', '陈经理', '0734-5555555', '耒阳市五一东路', '耒阳仁和大药房；税号91430481123456789Z', '启用', NULL),
('0004', '药剂科', '本院', '周药师', '0739-8888001', '邵阳市中医医院门诊楼1层药剂科', NULL, '启用', '院内科室'),
('0005', '骨科病房一', '本院', '刘护士长', '0739-8888011', '邵阳市中医医院住院部6楼骨科一病区', NULL, '启用', '院内科室'),
('0006', '骨科病房二', '本院', '张护士长', '0739-8888012', '邵阳市中医医院住院部7楼骨科二病区', NULL, '启用', '院内科室'),
('0007', '内科病房', '本院', '黄医生', '0739-8888021', '邵阳市中医医院住院部5楼内科病区', NULL, '启用', '院内科室');
