-- ============================================================
-- ERP系统数据库DDL脚本 v3（增量变更）
-- 基于schema_v2.sql新增功能表结构
-- 数据库: MySQL 8.0+  引擎: InnoDB  字符集: utf8mb4
-- ============================================================

-- ============================================================
-- 1. 生产详情 - 新增时间字段
-- 修改表：production_process_record
-- ============================================================

ALTER TABLE `production_process_record`
    ADD COLUMN `production_start` DATETIME DEFAULT NULL COMMENT '生产开始时间' AFTER `end_time`,
    ADD COLUMN `production_end`   DATETIME DEFAULT NULL COMMENT '生产结束时间' AFTER `production_start`,
    ADD COLUMN `inspection_start` DATETIME DEFAULT NULL COMMENT '检验开始时间' AFTER `production_end`,
    ADD COLUMN `inspection_end`   DATETIME DEFAULT NULL COMMENT '检验结束时间' AFTER `inspection_start`;

-- ============================================================
-- 2. 岗位信息
-- 新增表：position
-- ============================================================

CREATE TABLE `position` (
    `position_id`    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '岗位唯一标识',
    `position_code`  VARCHAR(255) NOT NULL COMMENT '岗位编码（唯一约束）',
    `position_name`  VARCHAR(255) NOT NULL COMMENT '岗位名称',
    `department_id`  BIGINT                DEFAULT NULL COMMENT '所属部门ID',
    `position_desc`  VARCHAR(255)          DEFAULT NULL COMMENT '岗位描述',
    `position_level` INT                   DEFAULT NULL COMMENT '岗位等级',
    `status`         INT          NOT NULL DEFAULT 1 COMMENT '状态：0停用/1启用',
    `sort_order`     INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `created_by`     BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updated_by`     BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`position_id`),
    UNIQUE KEY `uk_position_code` (`position_code`),
    CONSTRAINT `fk_pos_dept` FOREIGN KEY (`department_id`) REFERENCES `department` (`department_id`),
    CONSTRAINT `fk_pos_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_pos_updater` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位信息表';

-- ============================================================
-- 3. 人员档案 - 健康证管理
-- 新增表：personnel_file
-- ============================================================

CREATE TABLE `personnel_file` (
    `personnel_file_id`  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '人员档案唯一标识',
    `user_id`            BIGINT       NOT NULL COMMENT '关联用户ID',
    `position_id`        BIGINT                DEFAULT NULL COMMENT '岗位ID',
    `employee_no`        VARCHAR(255)          DEFAULT NULL COMMENT '工号',
    `id_card_no`         VARCHAR(255)          DEFAULT NULL COMMENT '身份证号',
    `health_cert_no`     VARCHAR(255)          DEFAULT NULL COMMENT '健康证编号',
    `health_cert_issue`  DATE                  DEFAULT NULL COMMENT '健康证发证日期',
    `health_cert_expire` DATE                  DEFAULT NULL COMMENT '健康证到期日期',
    `health_cert_file_id` BIGINT               DEFAULT NULL COMMENT '健康证附件文件ID',
    `education`          VARCHAR(255)          DEFAULT NULL COMMENT '学历',
    `major`              VARCHAR(255)          DEFAULT NULL COMMENT '专业',
    `entry_date`         DATE                  DEFAULT NULL COMMENT '入职日期',
    `status`             INT          NOT NULL DEFAULT 1 COMMENT '状态：0离职/1在职',
    `remark`             VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `created_by`         BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updated_by`         BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`            INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`personnel_file_id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_employee_no` (`employee_no`),
    KEY `idx_health_cert_expire` (`health_cert_expire`),
    CONSTRAINT `pf_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
    CONSTRAINT `pf_position` FOREIGN KEY (`position_id`) REFERENCES `position` (`position_id`),
    CONSTRAINT `pf_health_file` FOREIGN KEY (`health_cert_file_id`) REFERENCES `file_info` (`file_id`),
    CONSTRAINT `pf_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`),
    CONSTRAINT `pf_updater` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员档案表';

-- ============================================================
-- 4. 设备管理 - 维保记录
-- 新增表：equipment_maintenance
-- ============================================================

CREATE TABLE `equipment_maintenance` (
    `maintenance_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '维保记录唯一标识',
    `equipment_id`        BIGINT       NOT NULL COMMENT '关联设备ID',
    `maintenance_type`    VARCHAR(50)  NOT NULL COMMENT '维保类型：日常保养/定期检修/故障维修',
    `maintenance_date`    DATE         NOT NULL COMMENT '维保日期',
    `next_maintenance_date` DATE              DEFAULT NULL COMMENT '下次维保日期',
    `maintenance_content` TEXT                  DEFAULT NULL COMMENT '维保内容',
    `maintenance_result`  VARCHAR(255)          DEFAULT NULL COMMENT '维保结果',
    `cost`                DECIMAL(18,2)         DEFAULT NULL COMMENT '维保费用',
    `maintainer`          VARCHAR(255)          DEFAULT NULL COMMENT '维保人员',
    `remark`              VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `created_by`          BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updated_by`          BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`             INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`maintenance_id`),
    KEY `idx_equipment_id` (`equipment_id`),
    KEY `idx_next_maintenance` (`next_maintenance_date`),
    CONSTRAINT `fk_em_equipment` FOREIGN KEY (`equipment_id`) REFERENCES `equipment` (`equipment_id`),
    CONSTRAINT `fk_em_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_em_updater` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备维保记录表';

-- ============================================================
-- 5. 制剂信息 - 工序模版
-- 新增表：preparation_process_template
-- ============================================================

CREATE TABLE `preparation_process_template` (
    `template_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模版唯一标识',
    `preparation_id`   BIGINT       NOT NULL COMMENT '制剂ID',
    `process_type_id`  BIGINT       NOT NULL COMMENT '工序类型ID',
    `step_order`       INT          NOT NULL COMMENT '工序顺序',
    `standard_qty`     DECIMAL(18,2)         DEFAULT NULL COMMENT '标准加工数量',
    `unit_id`          BIGINT                DEFAULT NULL COMMENT '计量单位ID',
    `standard_duration` INT                  DEFAULT NULL COMMENT '标准工时（分钟）',
    `equipment_desc`   VARCHAR(255)          DEFAULT NULL COMMENT '设备要求描述',
    `room_desc`        VARCHAR(255)          DEFAULT NULL COMMENT '配置室要求',
    `remark`           VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `created_by`       BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updated_by`       BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`          INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`template_id`),
    KEY `idx_preparation_id` (`preparation_id`),
    CONSTRAINT `fk_ppt_prep` FOREIGN KEY (`preparation_id`) REFERENCES `preparation` (`preparation_id`),
    CONSTRAINT `fk_ppt_process_type` FOREIGN KEY (`process_type_id`) REFERENCES `process_type` (`process_id`),
    CONSTRAINT `fk_ppt_unit` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`unit_id`),
    CONSTRAINT `fk_ppt_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='制剂工序模版表';

-- ============================================================
-- 6. 制剂信息 - 文件上传
-- 新增表：preparation_document
-- ============================================================

CREATE TABLE `preparation_document` (
    `doc_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文档唯一标识',
    `preparation_id`  BIGINT       NOT NULL COMMENT '制剂ID',
    `doc_type`        VARCHAR(50)  NOT NULL COMMENT '文档类型：工艺规程/验证方案/验证报告模板/批记录模板',
    `doc_name`        VARCHAR(255) NOT NULL COMMENT '文档名称',
    `file_id`         BIGINT       NOT NULL COMMENT '关联文件ID',
    `version_no`      VARCHAR(50)           DEFAULT NULL COMMENT '版本号',
    `effective_date`  DATE                  DEFAULT NULL COMMENT '生效日期',
    `expire_date`     DATE                  DEFAULT NULL COMMENT '失效日期',
    `status`          INT          NOT NULL DEFAULT 1 COMMENT '状态：0作废/1有效',
    `created_by`      BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updated_by`      BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`doc_id`),
    KEY `idx_preparation_id` (`preparation_id`),
    KEY `idx_doc_type` (`doc_type`),
    CONSTRAINT `fk_pd_prep` FOREIGN KEY (`preparation_id`) REFERENCES `preparation` (`preparation_id`),
    CONSTRAINT `fk_pd_file` FOREIGN KEY (`file_id`) REFERENCES `file_info` (`file_id`),
    CONSTRAINT `fk_pd_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='制剂文档表';
