-- ============================================================
-- ERP系统数据库DDL脚本 v4（统一文件关联）
-- 新增business_file关联表，清理分散的file_id字段
-- 数据库: MySQL 8.0+  引擎: InnoDB  字符集: utf8mb4
-- ============================================================

-- ============================================================
-- 1. 新增表：business_file（业务文件关联表）
-- 统一管理所有业务表与文件的关联
-- ============================================================

CREATE TABLE `business_file` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `business_type`  VARCHAR(50)  NOT NULL COMMENT '业务类型：USER/PERSONNEL_FILE/PREPARATION/EQUIPMENT/PURCHASE_ORDER/STOCK_IN/STOCK_OUT/APPROVAL',
    `business_id`    BIGINT       NOT NULL COMMENT '业务记录ID',
    `file_id`        BIGINT       NOT NULL COMMENT '关联文件ID',
    `file_purpose`   VARCHAR(50)  NOT NULL DEFAULT 'ATTACHMENT' COMMENT '文件用途：HEALTH_CERT/PROCESS_SPEC/VALIDATION_PLAN/VALIDATION_REPORT/BATCH_RECORD/ATTACHMENT/IMAGE',
    `sort_order`     INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `created_by`     BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_file` (`business_type`, `business_id`, `file_id`, `file_purpose`),
    KEY `idx_business` (`business_type`, `business_id`),
    KEY `idx_file_id` (`file_id`),
    CONSTRAINT `fk_bf_file` FOREIGN KEY (`file_id`) REFERENCES `file_info` (`file_id`),
    CONSTRAINT `fk_bf_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务文件关联表';

-- ============================================================
-- 2. 清理personnel_file表 - 删除health_cert_file_id字段
-- 改用business_file关联健康证附件
-- ============================================================

ALTER TABLE `personnel_file`
    DROP FOREIGN KEY `pf_health_file`,
    DROP COLUMN `health_cert_file_id`;

-- ============================================================
-- 3. 清理preparation_document表 - 删除file_id字段
-- 改用business_file关联文档文件
-- ============================================================

ALTER TABLE `preparation_document`
    DROP FOREIGN KEY `fk_pd_file`,
    DROP COLUMN `file_id`;
