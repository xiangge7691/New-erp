-- ============================================================
-- ERP系统数据库DDL脚本 v2（修复版）
-- 基于schema.sql修复所有已知问题
-- 数据库: MySQL 8.0+  引擎: InnoDB  字符集: utf8mb4
-- ============================================================

-- ============================================================
-- 全局约定
-- 1. 主键统一为BIGINT AUTO_INCREMENT
-- 2. 所有业务表新增 is_deleted + version 字段
-- 3. 时间字段统一为 created_at / updated_at
-- 4. 所有字符串替代外键的字段改为外键关联
-- 5. 字符串冗余字段在有外键的情况下删除
-- ============================================================

-- ============================================================
-- 1. 用户权限管理模块
-- ============================================================

-- 用户表
CREATE TABLE `user` (
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
    `is_deleted`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
    `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_user_account` (`user_account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 角色表
CREATE TABLE `role` (
    `role_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色唯一标识',
    `role_name`   VARCHAR(255) NOT NULL COMMENT '角色名称（唯一约束）',
    `role_desc`   VARCHAR(255)          DEFAULT NULL COMMENT '角色描述',
    `role_status` INT          NOT NULL DEFAULT 1 COMMENT '状态：1启用/0禁用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
    `version`     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`role_id`),
    UNIQUE KEY `uk_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 权限表
CREATE TABLE `permission` (
    `perm_id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限唯一标识',
    `perm_key`       VARCHAR(255) NOT NULL COMMENT '权限键（唯一标识符）',
    `perm_name`      VARCHAR(255)          DEFAULT NULL COMMENT '权限名称',
    `perm_type`      VARCHAR(50)           DEFAULT NULL COMMENT '权限类型',
    `parent_id`      BIGINT       NOT NULL DEFAULT 0 COMMENT '父权限ID（树形结构）',
    `display_order`  INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `perm_status`    INT          NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
    `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`perm_id`),
    UNIQUE KEY `uk_perm_key` (`perm_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- 用户-角色关联表
CREATE TABLE `user_role` (
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

-- 角色-权限关联表
CREATE TABLE `role_perm` (
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

-- 部门表（新增parent_id支持层级、status状态、sort_order排序）
CREATE TABLE `department` (
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

-- 用户-部门关联表
CREATE TABLE `user_department` (
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

-- 登录日志表（新增）
CREATE TABLE `login_log` (
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

-- 计量单位表（新增status）
CREATE TABLE `unit` (
    `unit_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '单位唯一标识',
    `unit_name`   VARCHAR(255) NOT NULL COMMENT '单位中文名称（法定全称）',
    `symbol`      VARCHAR(20)           DEFAULT NULL COMMENT '单位符号',
    `status`      INT          NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计量单位表';

-- 药品剂型分类表（新增status）
CREATE TABLE `dosage_form` (
    `dosage_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '剂型唯一标识',
    `dosage_name` VARCHAR(255) NOT NULL COMMENT '剂型名称（法定全称）',
    `remark`      VARCHAR(255)          DEFAULT NULL COMMENT '剂型特性备注',
    `status`      INT          NOT NULL DEFAULT 1 COMMENT '状态：0禁用/1启用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`dosage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品剂型分类表';

-- 物料分类表（新增，替代material.category_name字符串）
CREATE TABLE `material_category` (
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

-- 物料信息表（category_name→category_id外键，unit_name→unit_id外键）
CREATE TABLE `material` (
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

-- 房间表（主键统一为BIGINT）
CREATE TABLE `room_info` (
    `room_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '房间ID',
    `room_name`     VARCHAR(255) NOT NULL COMMENT '房间名',
    `room_location` VARCHAR(255)          DEFAULT NULL COMMENT '房间位置',
    `area`          DECIMAL(18,2)          DEFAULT NULL COMMENT '面积（㎡）',
    `remark`        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `room_status`   INT          NOT NULL DEFAULT 1 COMMENT '状态：1启用/0停用',
    `creator_id`    BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updater_id`    BIGINT                DEFAULT NULL COMMENT '修改人ID',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    `is_deleted`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`room_id`),
    CONSTRAINT `fk_ri_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_ri_updater` FOREIGN KEY (`updater_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间表';

-- 设备表（主键统一为BIGINT）
CREATE TABLE `equipment` (
    `equipment_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '设备ID',
    `equipment_name`        VARCHAR(255) NOT NULL COMMENT '设备名称',
    `equipment_model`       VARCHAR(255)          DEFAULT NULL COMMENT '设备型号',
    `room_id`       BIGINT                DEFAULT NULL COMMENT '所在房间ID',
    `production_capacity`   VARCHAR(255)          DEFAULT NULL COMMENT '生产能力',
    `equipment_status`      INT          NOT NULL DEFAULT 1 COMMENT '状态：1启用/0停用',
    `fixed_asset_code`      VARCHAR(255)          DEFAULT NULL COMMENT '固定资产编号',
    `manufacturer`          VARCHAR(255)          DEFAULT NULL COMMENT '生产厂家',
    `purchase_date`         DATE                   DEFAULT NULL COMMENT '购置时间',
    `purchase_amount`       DECIMAL(18,2)          DEFAULT NULL COMMENT '购置金额',
    `last_maintenance_date` DATE                   DEFAULT NULL COMMENT '上次维保时间',
    `remark`        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `creator_id`    BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updater_id`    BIGINT                DEFAULT NULL COMMENT '修改人ID',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    `is_deleted`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`equipment_id`),
    CONSTRAINT `fk_eq_room` FOREIGN KEY (`room_id`) REFERENCES `room_info` (`room_id`),
    CONSTRAINT `fk_eq_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`),
    CONSTRAINT `fk_eq_updater` FOREIGN KEY (`updater_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

-- 工序类型表（主键统一为BIGINT）
CREATE TABLE `process_type` (
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

-- 生产单位信息表
CREATE TABLE `production_unit` (
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

-- 生产单位发票信息表
CREATE TABLE `prod_unit_invoice` (
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

-- 生产单位材料文件表（file_content改为file_path，不再存Base64）
CREATE TABLE `prod_unit_material_file` (
    `prod_material_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '材料文件唯一标识',
    `prod_unit_id`     BIGINT       NOT NULL COMMENT '关联的生产单位ID',
    `material_type`    VARCHAR(50)           DEFAULT NULL COMMENT '材料文件类型',
    `file_name`        VARCHAR(255)          DEFAULT NULL COMMENT '文件名称',
    `file_md5`         VARCHAR(255)          DEFAULT NULL COMMENT '文件内容MD5哈希值',
    `file_size`        INT                   DEFAULT NULL COMMENT '文件大小（字节）',
    `file_path`        VARCHAR(500)          DEFAULT NULL COMMENT '文件存储路径',
    `description`      VARCHAR(255)          DEFAULT NULL COMMENT '文件描述',
    `created_by`       BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`prod_material_id`),
    KEY `idx_prod_unit_id` (`prod_unit_id`),
    CONSTRAINT `fk_pmf_unit` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
    CONSTRAINT `fk_pmf_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产单位材料文件表';

-- 制剂信息表（dosage_form→dosage_form_id外键，unit_name→unit_id外键，producer→prod_unit_id外键）
CREATE TABLE `preparation` (
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

-- 制剂处方信息表（删除冗余名称字段，保留外键关联）
CREATE TABLE `preparation_formula` (
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

-- 生产计划主表（主键统一为BIGINT，新增preparation_id外键，删除冗余字段）
CREATE TABLE `production_plan` (
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

-- 计划状态流水表（主键统一为BIGINT）
CREATE TABLE `plan_status_log` (
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

-- 生产工序记录表（process_name→process_type_id外键，operator_name→operator_id外键，equipment→equipment_id外键，workshop→room_id外键）
CREATE TABLE `production_process_record` (
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

-- 工单表（主键统一为BIGINT，新增status字段，producer→prod_unit_id外键，receiver→prod_unit_id外键）
CREATE TABLE `work_order` (
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

-- ============================================================
-- 4. 采购管理模块
-- ============================================================

-- 采购供应商信息表（新增created_at/updated_at）
CREATE TABLE `purchase_suppliers` (
    `id`              INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
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

-- 采购订单主表（新增created_by/updated_by/created_at/updated_at，新增supplier_id外键，warehouse/unit→prod_unit_id外键）
CREATE TABLE `purchase_orders` (
    `id`                        INT           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `purchase_number`           VARCHAR(255)  NOT NULL COMMENT '采购编号',
    `supplier_id`               INT                    DEFAULT NULL COMMENT '供应商ID',
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

-- 采购订单明细表（raw_material_name→material_id外键，unit→unit_id外键）
CREATE TABLE `purchase_order_items` (
    `id`                 INT           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id`           INT           NOT NULL COMMENT '关联订单ID',
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

-- 库存表（删除冗余字段，保留外键）
CREATE TABLE `stock` (
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

-- 入库单号序列表
CREATE TABLE `stock_in_sequence` (
    `seq_date`   DATE NOT NULL COMMENT '日期',
    `seq_number` INT  NOT NULL DEFAULT 0 COMMENT '序列号',
    PRIMARY KEY (`seq_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单号序列表';

-- 出库单号序列表
CREATE TABLE `stock_out_sequence` (
    `seq_date`   DATE NOT NULL COMMENT '日期',
    `seq_number` INT  NOT NULL DEFAULT 0 COMMENT '序列号',
    PRIMARY KEY (`seq_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单号序列表';

-- 入库主表（新增approval_instance_id关联）
CREATE TABLE `stock_in` (
    `in_id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '入库单唯一标识',
    `in_code`        VARCHAR(255)   NOT NULL COMMENT '入库单号（唯一）',
    `in_type`        VARCHAR(255)            DEFAULT NULL COMMENT '入库类型：采购/生产/退货/调拨/调整',
    `prod_unit_id`   BIGINT                  DEFAULT NULL COMMENT '入库仓库（生产单位ID）',
    `supplier_id`    INT                    DEFAULT NULL COMMENT '供应商ID',
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

-- 入库明细表（删除冗余字段）
CREATE TABLE `stock_in_detail` (
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

-- 出库主表（新增approval_instance_id关联）
CREATE TABLE `stock_out` (
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

-- 出库明细表（删除冗余字段）
CREATE TABLE `stock_out_detail` (
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

-- 库存交易记录表
CREATE TABLE `stock_transaction` (
    `transaction_id`    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '交易记录唯一标识',
    `stock_id`          BIGINT         NOT NULL COMMENT '关联库存ID',
    `transaction_type`  VARCHAR(50)             DEFAULT NULL COMMENT '交易类型：入库/出库/调整',
    `transaction_date`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
    `related_id`        BIGINT                  DEFAULT NULL COMMENT '关联单据ID',
    `related_type`      VARCHAR(50)             DEFAULT NULL COMMENT '关联单据类型',
    `quantity_before`   DECIMAL(18,2)           DEFAULT NULL COMMENT '交易前数量',
    `quantity_change`   DECIMAL(18,2)           DEFAULT NULL COMMENT '变动数量（正数增加，负数减少）',
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

-- 审批流程定义（新增status字段）
CREATE TABLE `approval_workflow` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `workflow_name` VARCHAR(255) NOT NULL COMMENT '流程名称',
    `workflow_type` VARCHAR(255)          DEFAULT NULL COMMENT '流程类型',
    `status`        INT          NOT NULL DEFAULT 1 COMMENT '状态：0停用/1启用',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程定义';

-- 审批节点定义（新增reject_to_node_id退回节点配置）
CREATE TABLE `approval_node` (
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

-- 审批实例（新增initiator_id发起人字段）
CREATE TABLE `approval_instance` (
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

-- 审批记录（新增approved_at审批时间）
CREATE TABLE `approval_record` (
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

-- 文件信息表
CREATE TABLE `file_info` (
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

-- ============================================================
-- 8. 操作审计日志表（新增）
-- ============================================================

CREATE TABLE `operation_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        BIGINT                DEFAULT NULL COMMENT '操作人ID',
    `module`         VARCHAR(255) NOT NULL COMMENT '操作模块',
    `action`         VARCHAR(255) NOT NULL COMMENT '操作类型（CREATE/UPDATE/DELETE/QUERY）',
    `target_type`    VARCHAR(255)          DEFAULT NULL COMMENT '操作对象类型',
    `target_id`      BIGINT                DEFAULT NULL COMMENT '操作对象ID',
    `description`    VARCHAR(255)          DEFAULT NULL COMMENT '操作描述',
    `request_method` VARCHAR(20)           DEFAULT NULL COMMENT '请求方法（GET/POST/PUT/DELETE）',
    `request_url`    VARCHAR(500)          DEFAULT NULL COMMENT '请求URL',
    `request_params` TEXT                  DEFAULT NULL COMMENT '请求参数',
    `response_code`  INT                   DEFAULT NULL COMMENT '响应状态码',
    `ip_address`     VARCHAR(255)          DEFAULT NULL COMMENT '操作IP',
    `duration_ms`    INT                   DEFAULT NULL COMMENT '执行耗时（毫秒）',
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS' COMMENT '操作状态：SUCCESS/FAIL',
    `error_message`  TEXT                  DEFAULT NULL COMMENT '错误信息',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_module_action` (`module`, `action`),
    CONSTRAINT `fk_ol_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';
