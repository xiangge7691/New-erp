-- ============================================================
-- ERP系统数据库DDL脚本 - 最终统一版
-- 基于Entity类定义，统一所有字段命名和类型
-- 数据库: MySQL 8.0+  引擎: InnoDB  字符集: utf8mb4
-- 主键统一: BIGINT AUTO_INCREMENT
-- 时间字段统一: created_at / updated_at
-- 软删除统一: is_deleted TINYINT(1) DEFAULT 0
-- 乐观锁统一: version INT DEFAULT 0
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 用户权限管理模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `user` (
    `user_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识',
    `user_account`  VARCHAR(255) NOT NULL COMMENT '用户名（唯一性约束）',
    `user_name`     VARCHAR(255)          DEFAULT NULL COMMENT '真实姓名',
    `password`      VARCHAR(255) NOT NULL COMMENT '加密密码（Argon2）',
    `phone`         VARCHAR(255)          DEFAULT NULL COMMENT '联系电话',
    `email`         VARCHAR(255)          DEFAULT NULL COMMENT '邮箱',
    `gender`        TINYINT               DEFAULT NULL COMMENT '性别：0女/1男/2未知',
    `user_status`   INT          NOT NULL DEFAULT 1 COMMENT '账户状态：0禁用/1启用',
    `user_notes`    VARCHAR(255)          DEFAULT NULL COMMENT '用户备注信息',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_user_account` (`user_account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

CREATE TABLE IF NOT EXISTS `role` (
    `role_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色唯一标识',
    `role_name`   VARCHAR(255) NOT NULL COMMENT '角色名称（唯一约束）',
    `role_desc`   VARCHAR(255)          DEFAULT NULL COMMENT '角色描述',
    `role_status` INT          NOT NULL DEFAULT 1 COMMENT '状态：1启用/0禁用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`role_id`),
    UNIQUE KEY `uk_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS `permission` (
    `perm_id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限唯一标识',
    `perm_key`       VARCHAR(255) NOT NULL COMMENT '权限键（唯一标识符）',
    `perm_name`      VARCHAR(255)          DEFAULT NULL COMMENT '权限名称',
    `perm_type`      VARCHAR(50)           DEFAULT NULL COMMENT '权限类型',
    `parent_id`      BIGINT       NOT NULL DEFAULT 0 COMMENT '父权限ID（树形结构）',
    `display_order`  INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `perm_status`    INT          NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`perm_id`),
    UNIQUE KEY `uk_perm_key` (`perm_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

CREATE TABLE IF NOT EXISTS `user_role` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `role_id`     BIGINT   NOT NULL COMMENT '角色ID',
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`  TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

CREATE TABLE IF NOT EXISTS `role_perm` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id`     BIGINT   NOT NULL COMMENT '角色ID',
    `perm_id`     BIGINT   NOT NULL COMMENT '权限ID',
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`  TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `perm_id`),
    CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`),
    CONSTRAINT `fk_rp_perm` FOREIGN KEY (`perm_id`) REFERENCES `permission` (`perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表';

CREATE TABLE IF NOT EXISTS `department` (
    `department_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门唯一标识',
    `department_name` VARCHAR(255) NOT NULL COMMENT '部门名称',
    `parent_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '父部门ID（0为顶级）',
    `status`          INT          NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用',
    `sort_order`      INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`department_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门信息表';

CREATE TABLE IF NOT EXISTS `user_department` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT   NOT NULL COMMENT '用户ID',
    `department_id`   BIGINT   NOT NULL COMMENT '部门ID',
    `is_primary`      TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主部门',
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`      TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_dept` (`user_id`, `department_id`),
    CONSTRAINT `fk_ud_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_ud_dept` FOREIGN KEY (`department_id`) REFERENCES `department` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-部门关联表';

CREATE TABLE IF NOT EXISTS `login_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
    `login_ip`     VARCHAR(255)          DEFAULT NULL COMMENT '登录IP',
    `login_device` VARCHAR(255)          DEFAULT NULL COMMENT '登录设备/浏览器',
    `login_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `login_status` INT          NOT NULL COMMENT '登录状态：0失败/1成功',
    `fail_reason`  VARCHAR(255)          DEFAULT NULL COMMENT '失败原因',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_login_time` (`login_time`),
    CONSTRAINT `fk_ll_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ============================================================
-- 2. 物料/基础数据模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `unit` (
    `unit_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '单位唯一标识',
    `unit_name`   VARCHAR(255) NOT NULL COMMENT '单位中文名称（法定全称）',
    `symbol`      VARCHAR(20)           DEFAULT NULL COMMENT '单位符号',
    `status`      INT          NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计量单位表';

CREATE TABLE IF NOT EXISTS `dosage_form` (
    `dosage_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '剂型唯一标识',
    `dosage_name` VARCHAR(255) NOT NULL COMMENT '剂型名称（法定全称）',
    `remark`      VARCHAR(255)          DEFAULT NULL COMMENT '剂型特性备注',
    `status`      INT          NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`dosage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品剂型分类表';

CREATE TABLE IF NOT EXISTS `material_category` (
    `category_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类唯一标识',
    `category_name` VARCHAR(255) NOT NULL COMMENT '分类名称（原料/辅料/包材等）',
    `parent_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '父分类ID（0为顶级）',
    `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`        INT          NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`category_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料分类表';

CREATE TABLE IF NOT EXISTS `material` (
    `material_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '物料唯一标识符',
    `material_code`   VARCHAR(255) NOT NULL COMMENT '物料编码（唯一性约束）',
    `material_name`   VARCHAR(255) NOT NULL COMMENT '物料名称',
    `category_id`     BIGINT                DEFAULT NULL COMMENT '分类ID',
    `unit_id`         BIGINT                DEFAULT NULL COMMENT '计量单位ID',
    `spec`            VARCHAR(255)          DEFAULT NULL COMMENT '规格描述',
    `material_status` INT          NOT NULL DEFAULT 1 COMMENT '状态：1启用/0禁用',
    `remark`          VARCHAR(255)          DEFAULT NULL COMMENT '备注信息',
    `created_by`      BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updated_by`      BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`material_id`),
    UNIQUE KEY `uk_material_code` (`material_code`),
    CONSTRAINT `fk_mat_category` FOREIGN KEY (`category_id`) REFERENCES `material_category` (`category_id`),
    CONSTRAINT `fk_mat_unit` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料信息表';

-- ============================================================
-- 3. 生产管理模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `room_info` (
    `room_id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '房间ID',
    `room_name`             VARCHAR(255) NOT NULL COMMENT '房间名',
    `room_location`         VARCHAR(255)          DEFAULT NULL COMMENT '房间位置',
    `area`                  DECIMAL(18,2)          DEFAULT NULL COMMENT '面积（㎡）',
    `production_type`       VARCHAR(255)          DEFAULT NULL COMMENT '生产制剂类型',
    `dust_room`             TINYINT(1)            DEFAULT 0 COMMENT '是否为产尘操作间',
    `clean_area`            TINYINT(1)            DEFAULT 0 COMMENT '是否为洁净区',
    `clean_grade`           VARCHAR(50)           DEFAULT NULL COMMENT '洁净等级',
    `clean_procedure_file_id` BIGINT              DEFAULT NULL COMMENT '洁净规程文件ID',
    `remark`                VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `room_status`           INT          NOT NULL DEFAULT 1 COMMENT '状态：1启用/0停用',
    `creator_id`            BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updater_id`            BIGINT                DEFAULT NULL COMMENT '修改人ID',
    `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    `is_deleted`            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`               INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`room_id`),
    CONSTRAINT `fk_ri_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_ri_updater` FOREIGN KEY (`updater_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间表';

CREATE TABLE IF NOT EXISTS `equipment` (
    `equipment_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '设备ID',
    `equipment_name`        VARCHAR(255) NOT NULL COMMENT '设备名称',
    `equipment_model`       VARCHAR(255)          DEFAULT NULL COMMENT '设备型号',
    `room_id`               BIGINT                DEFAULT NULL COMMENT '所在房间ID',
    `production_capacity`   VARCHAR(255)          DEFAULT NULL COMMENT '生产能力',
    `equipment_status`      INT          NOT NULL DEFAULT 1 COMMENT '状态：1启用/0停用',
    `fixed_asset_code`      VARCHAR(255)          DEFAULT NULL COMMENT '固定资产编号',
    `manufacturer`          VARCHAR(255)          DEFAULT NULL COMMENT '生产厂家',
    `purchase_date`         DATE                   DEFAULT NULL COMMENT '购置时间',
    `purchase_amount`       DECIMAL(18,2)          DEFAULT NULL COMMENT '购置金额',
    `last_maintenance_date` DATE                   DEFAULT NULL COMMENT '上次维保时间',
    `equipment_type`        VARCHAR(50)           DEFAULT NULL COMMENT '设备类型',
    `maintenance_cycle`     INT                   DEFAULT 6 COMMENT '维保周期（月）',
    `reminder_days`         INT                   DEFAULT 15 COMMENT '到期提醒天数',
    `next_maintenance_date` DATE                   DEFAULT NULL COMMENT '下次维保时间',
    `remark`                VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `creator_id`            BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updater_id`            BIGINT                DEFAULT NULL COMMENT '修改人ID',
    `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    `is_deleted`            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`               INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`equipment_id`),
    CONSTRAINT `fk_eq_room` FOREIGN KEY (`room_id`) REFERENCES `room_info` (`room_id`),
    CONSTRAINT `fk_eq_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_eq_updater` FOREIGN KEY (`updater_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

CREATE TABLE IF NOT EXISTS `process_type` (
    `process_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '工序类型ID',
    `process_code`        VARCHAR(255) NOT NULL COMMENT '工序类型编码',
    `process_name`        VARCHAR(255) NOT NULL COMMENT '工序类型名称',
    `process_description` VARCHAR(255)          DEFAULT NULL COMMENT '工序类型说明',
    `process_status`      INT          NOT NULL DEFAULT 1 COMMENT '状态：1启用/0未启用',
    `creator_id`          BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updater_id`          BIGINT                DEFAULT NULL COMMENT '修改人ID',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    `is_deleted`          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`             INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`process_id`),
    UNIQUE KEY `uk_process_code` (`process_code`),
    CONSTRAINT `fk_pt_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_pt_updater` FOREIGN KEY (`updater_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序类型表';

CREATE TABLE IF NOT EXISTS `production_unit` (
    `prod_unit_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '生产单位唯一标识',
    `prod_unit_code`    VARCHAR(255) NOT NULL COMMENT '生产单位编号（唯一性约束）',
    `prod_unit_name`    VARCHAR(255) NOT NULL COMMENT '生产单位名称',
    `prod_unit_address` VARCHAR(255)          DEFAULT NULL COMMENT '生产单位地址',
    `prod_unit_manager` VARCHAR(255)          DEFAULT NULL COMMENT '负责人姓名',
    `prod_unit_phone`   VARCHAR(255)          DEFAULT NULL COMMENT '联系电话',
    `prod_unit_status`  INT          NOT NULL DEFAULT 1 COMMENT '状态：0停用/1启用',
    `prod_unit_remark`  VARCHAR(255)          DEFAULT NULL COMMENT '备注信息',
    `created_by`        BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updated_by`        BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`           INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`prod_unit_id`),
    UNIQUE KEY `uk_prod_unit_code` (`prod_unit_code`),
    CONSTRAINT `fk_pu_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_pu_updater` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产单位信息表';

CREATE TABLE IF NOT EXISTS `prod_unit_invoice` (
    `prod_invoice_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '发票信息唯一标识',
    `prod_unit_id`      BIGINT       NOT NULL COMMENT '关联的生产单位ID',
    `prod_invoice_info` VARCHAR(255)          DEFAULT NULL COMMENT '发票信息内容',
    `created_by`        BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updated_by`        BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`prod_invoice_id`),
    KEY `idx_prod_unit_id` (`prod_unit_id`),
    CONSTRAINT `fk_pui_unit` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
    CONSTRAINT `fk_pui_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产单位发票信息表';

CREATE TABLE IF NOT EXISTS `prod_unit_material_file` (
    `prod_material_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '材料文件唯一标识',
    `prod_unit_id`     BIGINT       NOT NULL COMMENT '关联的生产单位ID',
    `material_type`    VARCHAR(50)           DEFAULT NULL COMMENT '材料文件类型',
    `file_name`        VARCHAR(255)          DEFAULT NULL COMMENT '文件名称',
    `file_md5`         VARCHAR(255)          DEFAULT NULL COMMENT '文件内容MD5哈希值',
    `file_size`        INT                   DEFAULT NULL COMMENT '文件大小（字节）',
    `file_path`        VARCHAR(500)          DEFAULT NULL COMMENT '文件存储路径',
    `file_content`     MEDIUMTEXT            DEFAULT NULL COMMENT '文件内容（Base64编码）',
    `description`      VARCHAR(255)          DEFAULT NULL COMMENT '文件描述',
    `created_by`       BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`prod_material_id`),
    KEY `idx_prod_unit_id` (`prod_unit_id`),
    CONSTRAINT `fk_pmf_unit` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
    CONSTRAINT `fk_pmf_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产单位材料文件表';

CREATE TABLE IF NOT EXISTS `preparation` (
    `preparation_id`    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '制剂唯一标识',
    `preparation_code`  VARCHAR(255)   NOT NULL COMMENT '制剂编码（唯一性约束）',
    `preparation_name`  VARCHAR(255)   NOT NULL COMMENT '制剂品名',
    `spec`              VARCHAR(255)            DEFAULT NULL COMMENT '规格描述',
    `process_attr`      VARCHAR(255)            DEFAULT NULL COMMENT '加工性质',
    `package_spec`      VARCHAR(255)            DEFAULT NULL COMMENT '包装规格',
    `record_info`       VARCHAR(255)            DEFAULT NULL COMMENT '制剂备案',
    `function_main`     VARCHAR(255)            DEFAULT NULL COMMENT '功能主治',
    `method`            VARCHAR(255)            DEFAULT NULL COMMENT '制法',
    `unit_id`           BIGINT                  DEFAULT NULL COMMENT '单位ID',
    `dosage_form_id`    BIGINT                  DEFAULT NULL COMMENT '剂型ID',
    `prod_unit_id`      BIGINT                  DEFAULT NULL COMMENT '生产单位ID',
    `batch_qty`         DECIMAL(18,2)           DEFAULT NULL COMMENT '批量',
    `invoice_price`     DECIMAL(18,2)           DEFAULT NULL COMMENT '开票单价',
    `insurance_price`   DECIMAL(18,2)           DEFAULT NULL COMMENT '医保单价',
    `settlement_price`  DECIMAL(18,2)           DEFAULT NULL COMMENT '结算单价',
    `status`            INT          NOT NULL DEFAULT 1 COMMENT '状态：1启用/0禁用',
    `created_by`        BIGINT                  DEFAULT NULL COMMENT '创建人ID',
    `updated_by`        BIGINT                  DEFAULT NULL COMMENT '更新人ID',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`           INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`preparation_id`),
    UNIQUE KEY `uk_preparation_code` (`preparation_code`),
    CONSTRAINT `fk_prep_unit` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`unit_id`),
    CONSTRAINT `fk_prep_dosage` FOREIGN KEY (`dosage_form_id`) REFERENCES `dosage_form` (`dosage_id`),
    CONSTRAINT `fk_prep_prod_unit` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
    CONSTRAINT `fk_prep_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_prep_updater` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='制剂信息表';

CREATE TABLE IF NOT EXISTS `preparation_formula` (
    `formula_id`        BIGINT         NOT NULL AUTO_INCREMENT COMMENT '处方明细唯一标识',
    `preparation_id`    BIGINT         NOT NULL COMMENT '制剂ID',
    `material_id`       BIGINT                  DEFAULT NULL COMMENT '原料ID',
    `dosage`            DECIMAL(18,2)           DEFAULT NULL COMMENT '处方量',
    `unit_id`           BIGINT                  DEFAULT NULL COMMENT '单位ID',
    `created_by`        BIGINT                  DEFAULT NULL COMMENT '创建人ID',
    `updated_by`        BIGINT                  DEFAULT NULL COMMENT '更新人ID',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`formula_id`),
    KEY `idx_preparation_id` (`preparation_id`),
    CONSTRAINT `fk_pf_prep` FOREIGN KEY (`preparation_id`) REFERENCES `preparation` (`preparation_id`),
    CONSTRAINT `fk_pf_material` FOREIGN KEY (`material_id`) REFERENCES `material` (`material_id`),
    CONSTRAINT `fk_pf_unit` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`unit_id`),
    CONSTRAINT `fk_pf_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='制剂处方信息表';

CREATE TABLE IF NOT EXISTS `production_plan` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '计划唯一标识',
    `plan_number`           VARCHAR(255)          DEFAULT NULL COMMENT '计划编号',
    `related_order`         VARCHAR(255)          DEFAULT NULL COMMENT '关联单号（销售订单号）',
    `preparation_id`        BIGINT                DEFAULT NULL COMMENT '制剂ID',
    `plan_quantity`         DECIMAL(18,2)          DEFAULT NULL COMMENT '计划数量（批量）',
    `plan_type`             VARCHAR(50)            DEFAULT NULL COMMENT '生产计划类型',
    `current_status`        VARCHAR(255)           DEFAULT NULL COMMENT '当前状态',
    `current_status_date`   DATETIME               DEFAULT NULL COMMENT '当前状态时间',
    `unit_id`               BIGINT                DEFAULT NULL COMMENT '单位ID',
    `prod_unit_id`          BIGINT                DEFAULT NULL COMMENT '生产单位ID',
    `unit_price`            DECIMAL(18,2)          DEFAULT NULL COMMENT '单价',
    `finished_quantity`     DECIMAL(18,2)          DEFAULT NULL COMMENT '成品数量',
    `production_cycle`      INT                    DEFAULT NULL COMMENT '周期（天）',
    `yield_rate`            DECIMAL(18,2)          DEFAULT NULL COMMENT '得率（百分比）',
    `total_amount`          DECIMAL(18,2)          DEFAULT NULL COMMENT '总金额',
    `production_start_time` DATETIME               DEFAULT NULL COMMENT '生产开始时间',
    `production_end_time`   DATETIME               DEFAULT NULL COMMENT '生产结束时间',
    `inspection_start_time` DATETIME               DEFAULT NULL COMMENT '检验开始时间',
    `inspection_end_time`   DATETIME               DEFAULT NULL COMMENT '检验结束时间',
    `outbound_time`         DATETIME               DEFAULT NULL COMMENT '出库时间',
    `archive_time`          DATETIME               DEFAULT NULL COMMENT '归档时间',
    `remark`                VARCHAR(255)           DEFAULT NULL COMMENT '备注信息',
    `is_archived`           INT          NOT NULL DEFAULT 0 COMMENT '是否已归档（0-未归档，1-已归档）',
    `create_user`           BIGINT                 DEFAULT NULL COMMENT '创建人员ID',
    `update_user`           BIGINT                 DEFAULT NULL COMMENT '最后更新人员ID',
    `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `is_deleted`            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`               INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_pp_prep` FOREIGN KEY (`preparation_id`) REFERENCES `preparation` (`preparation_id`),
    CONSTRAINT `fk_pp_unit` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`unit_id`),
    CONSTRAINT `fk_pp_prod_unit` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
    CONSTRAINT `fk_pp_create_user` FOREIGN KEY (`create_user`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_pp_update_user` FOREIGN KEY (`update_user`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产计划主表';

CREATE TABLE IF NOT EXISTS `plan_status_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    `plan_id`     BIGINT       NOT NULL COMMENT '关联生产计划ID',
    `from_status` VARCHAR(255)          DEFAULT NULL COMMENT '变更前状态',
    `to_status`   VARCHAR(255)          DEFAULT NULL COMMENT '变更后状态',
    `change_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '状态变更时间',
    `operator`    BIGINT                DEFAULT NULL COMMENT '操作人员ID',
    `remark`      VARCHAR(255)          DEFAULT NULL COMMENT '变更原因或备注',
    PRIMARY KEY (`id`),
    KEY `idx_plan_id` (`plan_id`),
    CONSTRAINT `fk_psl_plan` FOREIGN KEY (`plan_id`) REFERENCES `production_plan` (`id`),
    CONSTRAINT `fk_psl_operator` FOREIGN KEY (`operator`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划状态流水表';

CREATE TABLE IF NOT EXISTS `production_process_record` (
    `record_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `plan_id`         BIGINT       NOT NULL COMMENT '生产计划ID',
    `process_type_id` BIGINT                DEFAULT NULL COMMENT '工序类型ID',
    `operator_id`     BIGINT                DEFAULT NULL COMMENT '操作人ID',
    `step_order`      INT                   DEFAULT NULL COMMENT '工序顺序',
    `room_id`         BIGINT                DEFAULT NULL COMMENT '配置室ID',
    `processing_qty`  DECIMAL(18,2)          DEFAULT NULL COMMENT '加工数量',
    `unit_id`         BIGINT                DEFAULT NULL COMMENT '计量单位ID',
    `equipment_id`    BIGINT                DEFAULT NULL COMMENT '使用设备ID',
    `start_time`      DATETIME               DEFAULT NULL COMMENT '工序开始时间',
    `end_time`        DATETIME               DEFAULT NULL COMMENT '工序结束时间',
    `production_start` DATETIME              DEFAULT NULL COMMENT '生产开始时间',
    `production_end`   DATETIME              DEFAULT NULL COMMENT '生产结束时间',
    `inspection_start` DATETIME              DEFAULT NULL COMMENT '检验开始时间',
    `inspection_end`   DATETIME              DEFAULT NULL COMMENT '检验结束时间',
    `record_status`   INT          NOT NULL DEFAULT 1 COMMENT '记录状态：1正常/0作废',
    `remark`          VARCHAR(255)           DEFAULT NULL COMMENT '备注信息',
    `creator_id`      BIGINT                 DEFAULT NULL COMMENT '创建人ID',
    `updater_id`      BIGINT                 DEFAULT NULL COMMENT '修改人ID',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME               DEFAULT NULL COMMENT '最后修改时间',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`record_id`),
    KEY `idx_plan_id` (`plan_id`),
    CONSTRAINT `fk_ppr_plan` FOREIGN KEY (`plan_id`) REFERENCES `production_plan` (`id`),
    CONSTRAINT `fk_ppr_process_type` FOREIGN KEY (`process_type_id`) REFERENCES `process_type` (`process_id`),
    CONSTRAINT `fk_ppr_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_ppr_room` FOREIGN KEY (`room_id`) REFERENCES `room_info` (`room_id`),
    CONSTRAINT `fk_ppr_unit` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`unit_id`),
    CONSTRAINT `fk_ppr_equipment` FOREIGN KEY (`equipment_id`) REFERENCES `equipment` (`equipment_id`),
    CONSTRAINT `fk_ppr_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产工序记录表';

CREATE TABLE IF NOT EXISTS `work_order` (
    `work_order_id`      BIGINT         NOT NULL AUTO_INCREMENT COMMENT '工单唯一标识',
    `work_order_code`    VARCHAR(255)   NOT NULL COMMENT '工单编号（唯一性约束）',
    `work_order_name`    VARCHAR(255)            DEFAULT NULL COMMENT '工单名称',
    `preparation_id`     BIGINT                  DEFAULT NULL COMMENT '制剂ID',
    `batch_qty`          DECIMAL(18,2)           DEFAULT NULL COMMENT '批量',
    `producer_unit_id`   BIGINT                  DEFAULT NULL COMMENT '生产单位ID',
    `receiver_unit_id`   BIGINT                  DEFAULT NULL COMMENT '收货单位ID',
    `delivery_time`      DATETIME                DEFAULT NULL COMMENT '交付时间',
    `invoice_price`      DECIMAL(18,2)           DEFAULT NULL COMMENT '开票单价',
    `insurance_price`    DECIMAL(18,2)           DEFAULT NULL COMMENT '医保单价',
    `settlement_price`   DECIMAL(18,2)           DEFAULT NULL COMMENT '结算单价',
    `batch_number`       VARCHAR(255)            DEFAULT NULL COMMENT '批号',
    `outbound_qty`       DECIMAL(18,2)           DEFAULT NULL COMMENT '出库量',
    `receipt_amount`     DECIMAL(18,2)           DEFAULT NULL COMMENT '收款金额',
    `actual_receipt_amount` DECIMAL(18,2)        DEFAULT NULL COMMENT '实收款',
    `invoice_amount`     DECIMAL(18,2)           DEFAULT NULL COMMENT '开票金额',
    `settlement_amount`  DECIMAL(18,2)           DEFAULT NULL COMMENT '结算金额',
    `return_amount`      DECIMAL(18,2)           DEFAULT NULL COMMENT '返款金额',
    `status`             VARCHAR(50)             DEFAULT NULL COMMENT '工单状态：待执行/执行中/已完成/已取消',
    `created_by`         BIGINT                  DEFAULT NULL COMMENT '创建人ID',
    `updated_by`         BIGINT                  DEFAULT NULL COMMENT '更新人ID',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`            INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`work_order_id`),
    UNIQUE KEY `uk_work_order_code` (`work_order_code`),
    CONSTRAINT `fk_wo_prep` FOREIGN KEY (`preparation_id`) REFERENCES `preparation` (`preparation_id`),
    CONSTRAINT `fk_wo_producer` FOREIGN KEY (`producer_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
    CONSTRAINT `fk_wo_receiver` FOREIGN KEY (`receiver_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
    CONSTRAINT `fk_wo_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_wo_updater` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

CREATE TABLE IF NOT EXISTS `position` (
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

CREATE TABLE IF NOT EXISTS `personnel_file` (
    `personnel_file_id`  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '人员档案唯一标识',
    `user_id`            BIGINT       NOT NULL COMMENT '关联用户ID',
    `position_id`        BIGINT                DEFAULT NULL COMMENT '岗位ID',
    `employee_no`        VARCHAR(255)          DEFAULT NULL COMMENT '工号',
    `name`               VARCHAR(50)           DEFAULT NULL COMMENT '姓名',
    `department_id`      BIGINT                DEFAULT NULL COMMENT '所属部门ID',
    `qualification`      VARCHAR(255)          DEFAULT NULL COMMENT '人员资格认定',
    `title_name`         VARCHAR(100)          DEFAULT NULL COMMENT '职称名',
    `title_level`        VARCHAR(50)           DEFAULT NULL COMMENT '职称等级',
    `id_card_no`         VARCHAR(255)          DEFAULT NULL COMMENT '身份证号',
    `education`          VARCHAR(255)          DEFAULT NULL COMMENT '学历',
    `major`              VARCHAR(255)          DEFAULT NULL COMMENT '专业',
    `entry_date`         DATE                  DEFAULT NULL COMMENT '入职日期',
    `work_experience`    VARCHAR(500)          DEFAULT NULL COMMENT '任职简历',
    `education_training` VARCHAR(500)          DEFAULT NULL COMMENT '教育-培训经历',
    `certificate_name`   VARCHAR(100)          DEFAULT NULL COMMENT '注册证书名',
    `certificate_no`     VARCHAR(50)           DEFAULT NULL COMMENT '注册证书号',
    `health_cert_no`     VARCHAR(255)          DEFAULT NULL COMMENT '健康证编号',
    `health_cert_issue`  DATE                  DEFAULT NULL COMMENT '健康证发证日期',
    `health_cert_expire` DATE                  DEFAULT NULL COMMENT '健康证到期日期',
    `last_checkup_date`  DATE                  DEFAULT NULL COMMENT '上次体检时间',
    `health_file`        BIGINT                DEFAULT NULL COMMENT '健康档案（关联file_info.file_id）',
    `attachments`        BIGINT                DEFAULT NULL COMMENT '附件（关联file_info.file_id）',
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
    CONSTRAINT `pf_department` FOREIGN KEY (`department_id`) REFERENCES `department` (`department_id`),
    CONSTRAINT `pf_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`),
    CONSTRAINT `pf_updater` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员档案表';

CREATE TABLE IF NOT EXISTS `preparation_process_template` (
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

CREATE TABLE IF NOT EXISTS `preparation_document` (
    `doc_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文档唯一标识',
    `preparation_id`  BIGINT       NOT NULL COMMENT '制剂ID',
    `doc_type`        VARCHAR(50)  NOT NULL COMMENT '文档类型：工艺规程/验证方案/验证报告模板/批记录模板',
    `doc_name`        VARCHAR(255) NOT NULL COMMENT '文档名称',
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
    CONSTRAINT `fk_pd_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='制剂文档表';

CREATE TABLE IF NOT EXISTS `equipment_maintenance` (
    `maintenance_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '维保记录唯一标识',
    `equipment_id`        BIGINT       NOT NULL COMMENT '关联设备ID',
    `maintenance_type`    VARCHAR(50)  NOT NULL COMMENT '维保类型：日常保养/定期检修/故障维修',
    `maintenance_date`    DATE         NOT NULL COMMENT '维保日期',
    `next_maintenance_date` DATE              DEFAULT NULL COMMENT '下次维保日期',
    `maintenance_content` TEXT                  DEFAULT NULL COMMENT '维保内容',
    `maintenance_result`  VARCHAR(255)          DEFAULT NULL COMMENT '维保结果',
    `cost`                DECIMAL(18,2)         DEFAULT NULL COMMENT '维保费用',
    `maintainer`          VARCHAR(255)          DEFAULT NULL COMMENT '维保人员',
    `maintenance_company` VARCHAR(255)          DEFAULT NULL COMMENT '维保公司',
    `contact_phone`       VARCHAR(255)          DEFAULT NULL COMMENT '联系方式',
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
-- 4. 采购管理模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `purchase_suppliers` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `supplier_number` VARCHAR(255) NOT NULL COMMENT '供应商编号',
    `supplier_name`   VARCHAR(255) NOT NULL COMMENT '供应商名称',
    `category`        VARCHAR(255)          DEFAULT NULL COMMENT '类别',
    `contact_person`  VARCHAR(255)          DEFAULT NULL COMMENT '联系人',
    `phone`           VARCHAR(255)          DEFAULT NULL COMMENT '手机号',
    `email`           VARCHAR(255)          DEFAULT NULL COMMENT '邮箱',
    `address`         VARCHAR(255)          DEFAULT NULL COMMENT '地址',
    `bank_account`    VARCHAR(255)          DEFAULT NULL COMMENT '银行账户',
    `bank_name`       VARCHAR(255)          DEFAULT NULL COMMENT '开户行',
    `remark`          VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `status`          VARCHAR(50)  NOT NULL DEFAULT '启用' COMMENT '状态',
    `material_info`   VARCHAR(255)          DEFAULT NULL COMMENT '材料信息',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_supplier_number` (`supplier_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购供应商信息表';

CREATE TABLE IF NOT EXISTS `purchase_orders` (
    `id`                        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `purchase_number`           VARCHAR(255)  NOT NULL COMMENT '采购编号',
    `supplier_id`               BIGINT                 DEFAULT NULL COMMENT '供应商ID',
    `prod_unit_id`              BIGINT                 DEFAULT NULL COMMENT '仓库（生产单位ID）',
    `processing_date`           DATE                   DEFAULT NULL COMMENT '处理日期',
    `expected_delivery_date`    DATE                   DEFAULT NULL COMMENT '预计到货日期',
    `invoice_info`              VARCHAR(255)           DEFAULT NULL COMMENT '发票信息',
    `receiving_info`            VARCHAR(255)           DEFAULT NULL COMMENT '收货信息',
    `title`                     VARCHAR(255)           DEFAULT NULL COMMENT '采购单标题',
    `prescription_multiple`     DECIMAL(18,2)          DEFAULT NULL COMMENT '处方倍数',
    `remark`                    VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    `generate_production_plan`  INT          NOT NULL DEFAULT 0 COMMENT '是否生成生产计划',
    `status`                    VARCHAR(50)            DEFAULT NULL COMMENT '状态',
    `approval_instance_id`      BIGINT                 DEFAULT NULL COMMENT '审批实例ID',
    `created_by`                BIGINT                 DEFAULT NULL COMMENT '创建人ID',
    `updated_by`                BIGINT                 DEFAULT NULL COMMENT '更新人ID',
    `created_at`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`                TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`                   INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_purchase_number` (`purchase_number`),
    CONSTRAINT `fk_po_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `purchase_suppliers` (`id`),
    CONSTRAINT `fk_po_prod_unit` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
    CONSTRAINT `fk_po_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_po_updater` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单主表';

CREATE TABLE IF NOT EXISTS `purchase_order_items` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id`           BIGINT        NOT NULL COMMENT '关联订单ID',
    `sequence_number`    INT                    DEFAULT NULL COMMENT '序号',
    `material_id`        BIGINT                 DEFAULT NULL COMMENT '物料ID',
    `dose`               DECIMAL(18,2)          DEFAULT NULL COMMENT '原药材剂量',
    `unit_id`            BIGINT                 DEFAULT NULL COMMENT '单位ID',
    `processing_property` VARCHAR(255)          DEFAULT NULL COMMENT '加工性质',
    `stock`              DECIMAL(18,2)          DEFAULT NULL COMMENT '库存',
    `purchase_quantity`  DECIMAL(18,2)          DEFAULT NULL COMMENT '采购数量',
    `difference`         DECIMAL(18,2)          DEFAULT NULL COMMENT '差值',
    `is_deleted`         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    CONSTRAINT `fk_poi_order` FOREIGN KEY (`order_id`) REFERENCES `purchase_orders` (`id`),
    CONSTRAINT `fk_poi_material` FOREIGN KEY (`material_id`) REFERENCES `material` (`material_id`),
    CONSTRAINT `fk_poi_unit` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单明细表';

-- ============================================================
-- 5. 库存管理模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `stock` (
    `stock_id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '库存唯一标识符',
    `prod_unit_id`      BIGINT                  DEFAULT NULL COMMENT '关联的生产单位ID',
    `item_type`         VARCHAR(50)             NOT NULL COMMENT '物品类型：material/preparation',
    `item_id`           BIGINT                  NOT NULL COMMENT '关联的物品ID',
    `category_id`       BIGINT                  DEFAULT NULL COMMENT '分类ID',
    `unit_id`           BIGINT                  DEFAULT NULL COMMENT '计量单位ID',
    `quantity`          DECIMAL(18,2)  NOT NULL DEFAULT 0 COMMENT '库存数量',
    `min_quantity`      DECIMAL(18,2)           DEFAULT NULL COMMENT '最低库存预警数量',
    `max_quantity`      DECIMAL(18,2)           DEFAULT NULL COMMENT '最高库存限制数量',
    `batch_number`      VARCHAR(255)            DEFAULT NULL COMMENT '批次号',
    `production_date`   DATE                    DEFAULT NULL COMMENT '生产日期',
    `expiry_date`       DATE                    DEFAULT NULL COMMENT '有效期至',
    `storage_location`  VARCHAR(255)            DEFAULT NULL COMMENT '库位/货架号',
    `stock_status`      VARCHAR(50)             DEFAULT NULL COMMENT '库存状态',
    `remark`            VARCHAR(255)            DEFAULT NULL COMMENT '备注信息',
    `created_by`        BIGINT                  DEFAULT NULL COMMENT '创建人ID',
    `updated_by`        BIGINT                  DEFAULT NULL COMMENT '更新人ID',
    `created_at`        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`           INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`stock_id`),
    UNIQUE KEY `uk_stock_item` (`prod_unit_id`, `item_type`, `item_id`, `batch_number`),
    KEY `idx_item_type_id` (`item_type`, `item_id`),
    CONSTRAINT `fk_st_prod_unit` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
    CONSTRAINT `fk_st_category` FOREIGN KEY (`category_id`) REFERENCES `material_category` (`category_id`),
    CONSTRAINT `fk_st_unit` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`unit_id`),
    CONSTRAINT `fk_st_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

CREATE TABLE IF NOT EXISTS `stock_in_sequence` (
    `seq_date`   DATE NOT NULL COMMENT '日期',
    `seq_number` INT  NOT NULL DEFAULT 0 COMMENT '序列号',
    PRIMARY KEY (`seq_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单号序列表';

CREATE TABLE IF NOT EXISTS `stock_out_sequence` (
    `seq_date`   DATE NOT NULL COMMENT '日期',
    `seq_number` INT  NOT NULL DEFAULT 0 COMMENT '序列号',
    PRIMARY KEY (`seq_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单号序列表';

CREATE TABLE IF NOT EXISTS `stock_in` (
    `in_id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '入库单唯一标识',
    `in_code`        VARCHAR(255)   NOT NULL COMMENT '入库单号（唯一）',
    `in_type`        VARCHAR(255)            DEFAULT NULL COMMENT '入库类型：采购/生产/退货/调拨/调整',
    `prod_unit_id`   BIGINT                  DEFAULT NULL COMMENT '入库仓库（生产单位ID）',
    `supplier_id`    BIGINT                  DEFAULT NULL COMMENT '供应商ID',
    `related_order`  VARCHAR(255)            DEFAULT NULL COMMENT '关联单号',
    `in_date`        DATE                    DEFAULT NULL COMMENT '入库日期',
    `total_amount`   DECIMAL(18,2)           DEFAULT NULL COMMENT '入库总金额',
    `in_status`      VARCHAR(255)            DEFAULT NULL COMMENT '状态：草稿/已确认/已完成/已取消',
    `remark`         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    `approval_instance_id` BIGINT            DEFAULT NULL COMMENT '审批实例ID',
    `created_by`     BIGINT                  DEFAULT NULL COMMENT '创建人ID',
    `updated_by`     BIGINT                  DEFAULT NULL COMMENT '更新人ID',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`        INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`in_id`),
    UNIQUE KEY `uk_in_code` (`in_code`),
    CONSTRAINT `fk_si_prod_unit` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
    CONSTRAINT `fk_si_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `purchase_suppliers` (`id`),
    CONSTRAINT `fk_si_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库主表';

CREATE TABLE IF NOT EXISTS `stock_in_detail` (
    `in_detail_id`    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '入库明细唯一标识',
    `in_id`           BIGINT         NOT NULL COMMENT '关联入库单ID',
    `item_type`       VARCHAR(50)             DEFAULT NULL COMMENT '物品类型',
    `item_id`         BIGINT                  DEFAULT NULL COMMENT '物品ID',
    `category_id`     BIGINT                  DEFAULT NULL COMMENT '分类ID',
    `unit_id`         BIGINT                  DEFAULT NULL COMMENT '单位ID',
    `batch_number`    VARCHAR(255)            DEFAULT NULL COMMENT '批次号',
    `production_date` DATE                    DEFAULT NULL COMMENT '生产日期',
    `expiry_date`     DATE                    DEFAULT NULL COMMENT '有效期至',
    `quantity`        DECIMAL(18,2)           DEFAULT NULL COMMENT '入库数量',
    `unit_price`      DECIMAL(18,2)           DEFAULT NULL COMMENT '单价',
    `amount`          DECIMAL(18,2)           DEFAULT NULL COMMENT '金额',
    `storage_location` VARCHAR(255)           DEFAULT NULL COMMENT '存放位置',
    `remark`          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    `is_deleted`      TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`in_detail_id`),
    KEY `idx_in_id` (`in_id`),
    CONSTRAINT `fk_sid_in` FOREIGN KEY (`in_id`) REFERENCES `stock_in` (`in_id`),
    CONSTRAINT `fk_sid_category` FOREIGN KEY (`category_id`) REFERENCES `material_category` (`category_id`),
    CONSTRAINT `fk_sid_unit` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库明细表';

CREATE TABLE IF NOT EXISTS `stock_out` (
    `out_id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '出库单唯一标识',
    `out_code`        VARCHAR(255)   NOT NULL COMMENT '出库单号（唯一）',
    `out_type`        VARCHAR(255)            DEFAULT NULL COMMENT '出库类型：销售/生产领用/退货/调拨/调整',
    `prod_unit_id`    BIGINT                  DEFAULT NULL COMMENT '出库仓库（生产单位ID）',
    `customer_id`     BIGINT                  DEFAULT NULL COMMENT '客户ID',
    `related_order`   VARCHAR(255)            DEFAULT NULL COMMENT '关联单号',
    `out_date`        DATE                    DEFAULT NULL COMMENT '出库日期',
    `total_amount`    DECIMAL(18,2)           DEFAULT NULL COMMENT '出库总金额',
    `out_status`      VARCHAR(255)            DEFAULT NULL COMMENT '状态：草稿/已确认/已完成/已取消',
    `remark`          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    `approval_instance_id` BIGINT             DEFAULT NULL COMMENT '审批实例ID',
    `created_by`      BIGINT                  DEFAULT NULL COMMENT '创建人ID',
    `updated_by`      BIGINT                  DEFAULT NULL COMMENT '更新人ID',
    `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`         INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`out_id`),
    UNIQUE KEY `uk_out_code` (`out_code`),
    CONSTRAINT `fk_so_prod_unit` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
    CONSTRAINT `fk_so_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库主表';

CREATE TABLE IF NOT EXISTS `stock_out_detail` (
    `out_detail_id`  BIGINT         NOT NULL AUTO_INCREMENT COMMENT '出库明细唯一标识',
    `out_id`         BIGINT         NOT NULL COMMENT '关联出库单ID',
    `stock_id`       BIGINT                  DEFAULT NULL COMMENT '关联库存记录ID',
    `item_type`      VARCHAR(50)             DEFAULT NULL COMMENT '物品类型',
    `item_id`        BIGINT                  DEFAULT NULL COMMENT '物品ID',
    `category_id`    BIGINT                  DEFAULT NULL COMMENT '分类ID',
    `unit_id`        BIGINT                  DEFAULT NULL COMMENT '单位ID',
    `batch_number`   VARCHAR(255)            DEFAULT NULL COMMENT '批次号',
    `quantity`       DECIMAL(18,2)           DEFAULT NULL COMMENT '出库数量',
    `unit_price`     DECIMAL(18,2)           DEFAULT NULL COMMENT '单价',
    `amount`         DECIMAL(18,2)           DEFAULT NULL COMMENT '金额',
    `remark`         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    `is_deleted`     TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`out_detail_id`),
    KEY `idx_out_id` (`out_id`),
    CONSTRAINT `fk_sod_out` FOREIGN KEY (`out_id`) REFERENCES `stock_out` (`out_id`),
    CONSTRAINT `fk_sod_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`stock_id`),
    CONSTRAINT `fk_sod_category` FOREIGN KEY (`category_id`) REFERENCES `material_category` (`category_id`),
    CONSTRAINT `fk_sod_unit` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库明细表';

CREATE TABLE IF NOT EXISTS `stock_transaction` (
    `transaction_id`    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '交易记录唯一标识',
    `stock_id`          BIGINT         NOT NULL COMMENT '关联库存ID',
    `transaction_type`  VARCHAR(50)             DEFAULT NULL COMMENT '交易类型：入库/出库/调整',
    `transaction_date`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
    `related_id`        BIGINT                  DEFAULT NULL COMMENT '关联单据ID',
    `related_type`      VARCHAR(50)             DEFAULT NULL COMMENT '关联单据类型',
    `quantity_before`   DECIMAL(18,2)           DEFAULT NULL COMMENT '交易前数量',
    `quantity_change`   DECIMAL(18,2)           DEFAULT NULL COMMENT '变动数量',
    `quantity_after`    DECIMAL(18,2)           DEFAULT NULL COMMENT '交易后数量',
    `batch_number`      VARCHAR(255)            DEFAULT NULL COMMENT '批次号',
    `remark`            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    `created_by`        BIGINT                  DEFAULT NULL COMMENT '操作人ID',
    `created_at`        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`        TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`transaction_id`),
    KEY `idx_stock_id` (`stock_id`),
    CONSTRAINT `fk_st2_stock` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`stock_id`),
    CONSTRAINT `fk_st2_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存交易记录表';

-- ============================================================
-- 6. 审批流程模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `approval_workflow` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `workflow_name` VARCHAR(255) NOT NULL COMMENT '流程名称',
    `workflow_type` VARCHAR(255)          DEFAULT NULL COMMENT '流程类型',
    `status`        INT          NOT NULL DEFAULT 1 COMMENT '状态：0停用/1启用',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程定义';

CREATE TABLE IF NOT EXISTS `approval_node` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `workflow_id`        BIGINT       NOT NULL COMMENT '流程ID',
    `node_name`          VARCHAR(255) NOT NULL COMMENT '节点名称',
    `node_order`         INT                   DEFAULT NULL COMMENT '节点顺序',
    `role_id`            BIGINT                DEFAULT NULL COMMENT '审批角色ID',
    `need_bind_business` TINYINT(1)            DEFAULT 0 COMMENT '是否需要绑定业务',
    `bind_type`          VARCHAR(255)          DEFAULT NULL COMMENT '创建绑定类型',
    `enter_node_prompt`  VARCHAR(255)          DEFAULT NULL COMMENT '进入节点提示',
    `after_pass_status`  VARCHAR(255)          DEFAULT NULL COMMENT '通过后业务状态',
    `reject_prompt`      VARCHAR(255)          DEFAULT NULL COMMENT '驳回提示',
    `after_reject_status` VARCHAR(255)         DEFAULT NULL COMMENT '驳回后业务状态',
    `reject_to_node_id`  BIGINT                DEFAULT NULL COMMENT '驳回退回目标节点ID',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    KEY `idx_workflow_id` (`workflow_id`),
    CONSTRAINT `fk_an_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `approval_workflow` (`id`),
    CONSTRAINT `fk_an_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`),
    CONSTRAINT `fk_an_reject_to` FOREIGN KEY (`reject_to_node_id`) REFERENCES `approval_node` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批节点定义';

CREATE TABLE IF NOT EXISTS `approval_instance` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `workflow_id`     BIGINT       NOT NULL COMMENT '流程ID',
    `initiator_id`    BIGINT                DEFAULT NULL COMMENT '发起人ID',
    `related_id`      BIGINT                DEFAULT NULL COMMENT '关联业务ID',
    `related_type`    VARCHAR(255)          DEFAULT NULL COMMENT '关联业务类型',
    `current_node_id` BIGINT                DEFAULT NULL COMMENT '当前节点ID',
    `status`          VARCHAR(50)  NOT NULL COMMENT '审批状态',
    `cancel_reason`   VARCHAR(255)          DEFAULT NULL COMMENT '作废原因',
    `cancelled_by`    BIGINT                DEFAULT NULL COMMENT '作废人ID',
    `cancelled_at`    DATETIME              DEFAULT NULL COMMENT '作废时间',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    KEY `idx_workflow_id` (`workflow_id`),
    KEY `idx_related` (`related_type`, `related_id`),
    KEY `idx_initiator` (`initiator_id`),
    CONSTRAINT `fk_ai_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `approval_workflow` (`id`),
    CONSTRAINT `fk_ai_initiator` FOREIGN KEY (`initiator_id`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_ai_cancelled_by` FOREIGN KEY (`cancelled_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批实例';

CREATE TABLE IF NOT EXISTS `approval_record` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `instance_id`    BIGINT       NOT NULL COMMENT '审批实例ID',
    `node_id`        BIGINT       NOT NULL COMMENT '节点ID',
    `approver_id`    BIGINT       NOT NULL COMMENT '审批人ID',
    `action`         VARCHAR(50)  NOT NULL COMMENT '审批动作',
    `target_node_id` BIGINT                DEFAULT NULL COMMENT '转交目标节点ID',
    `comment`        VARCHAR(255)          DEFAULT NULL COMMENT '审批意见',
    `approved_at`    DATETIME              DEFAULT NULL COMMENT '审批时间',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`),
    KEY `idx_instance_id` (`instance_id`),
    CONSTRAINT `fk_ar_instance` FOREIGN KEY (`instance_id`) REFERENCES `approval_instance` (`id`),
    CONSTRAINT `fk_ar_node` FOREIGN KEY (`node_id`) REFERENCES `approval_node` (`id`),
    CONSTRAINT `fk_ar_approver` FOREIGN KEY (`approver_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录';

-- ============================================================
-- 7. 文件管理模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `file_info` (
    `file_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件唯一标识',
    `original_name`   VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `stored_name`     VARCHAR(255) NOT NULL COMMENT '存储文件名',
    `file_path`       VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_size`       BIGINT                DEFAULT NULL COMMENT '文件大小（字节）',
    `content_type`    VARCHAR(255)          DEFAULT NULL COMMENT '文件类型/内容类型',
    `file_extension`  VARCHAR(255)          DEFAULT NULL COMMENT '文件扩展名',
    `file_md5`        VARCHAR(255)          DEFAULT NULL COMMENT '文件MD5哈希值',
    `category`        VARCHAR(255)          DEFAULT NULL COMMENT '文件分类',
    `description`     VARCHAR(255)          DEFAULT NULL COMMENT '文件描述',
    `file_url`        VARCHAR(500)          DEFAULT NULL COMMENT '访问URL',
    `storage_type`    VARCHAR(50)           DEFAULT NULL COMMENT '存储类型：LOCAL/CLOUD',
    `business_id`     BIGINT                DEFAULT NULL COMMENT '关联业务ID',
    `business_type`   VARCHAR(255)          DEFAULT NULL COMMENT '关联业务类型',
    `created_by`      BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`file_id`),
    KEY `idx_business` (`business_type`, `business_id`),
    CONSTRAINT `fk_fi_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

CREATE TABLE IF NOT EXISTS `business_file` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `business_type`  VARCHAR(50)  NOT NULL COMMENT '业务类型',
    `business_id`    BIGINT       NOT NULL COMMENT '业务记录ID',
    `file_id`        BIGINT       NOT NULL COMMENT '关联文件ID',
    `file_purpose`   VARCHAR(50)  NOT NULL DEFAULT 'ATTACHMENT' COMMENT '文件用途',
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
-- 8. 操作审计日志模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `operation_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        BIGINT                DEFAULT NULL COMMENT '操作人ID',
    `module`         VARCHAR(255) NOT NULL COMMENT '操作模块',
    `action`         VARCHAR(255) NOT NULL COMMENT '操作类型',
    `target_type`    VARCHAR(255)          DEFAULT NULL COMMENT '操作对象类型',
    `target_id`      BIGINT                DEFAULT NULL COMMENT '操作对象ID',
    `description`    VARCHAR(255)          DEFAULT NULL COMMENT '操作描述',
    `request_method` VARCHAR(20)           DEFAULT NULL COMMENT '请求方法',
    `request_url`    VARCHAR(500)          DEFAULT NULL COMMENT '请求URL',
    `request_params` TEXT                  DEFAULT NULL COMMENT '请求参数',
    `response_code`  INT                   DEFAULT NULL COMMENT '响应状态码',
    `ip_address`     VARCHAR(255)          DEFAULT NULL COMMENT '操作IP',
    `duration_ms`    INT                   DEFAULT NULL COMMENT '执行耗时（毫秒）',
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS' COMMENT '操作状态',
    `error_message`  TEXT                  DEFAULT NULL COMMENT '错误信息',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_module_action` (`module`, `action`),
    CONSTRAINT `fk_ol_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';

-- ============================================================
-- 9. 环境监测模块（新增）
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
