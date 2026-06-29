-- ============================================================
-- ERP系统数据库DDL脚本
-- 从实体类自动提取生成，用于项目重构
-- 数据库: MySQL 8.0+  引擎: InnoDB  字符集: utf8mb4
-- ============================================================

-- ============================================================
-- 1. 用户权限管理模块
-- ============================================================

-- 用户表
CREATE TABLE `user` (
    `user_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识',
    `user_account`  VARCHAR(255) NOT NULL COMMENT '用户名（唯一性约束）',
    `user_name`     VARCHAR(255)          DEFAULT NULL COMMENT '真实姓名',
    `password`      VARCHAR(255) NOT NULL COMMENT '加密密码',
    `phone`         VARCHAR(255)          DEFAULT NULL COMMENT '联系电话',
    `gender`        VARCHAR(50)           DEFAULT NULL COMMENT '性别',
    `user_status`   INT                   DEFAULT 1 COMMENT '账户状态：0禁用/1启用',
    `user_notes`    VARCHAR(255)          DEFAULT NULL COMMENT '用户备注信息',
    `created_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_user_account` (`user_account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 角色表
CREATE TABLE `role` (
    `role_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色唯一标识',
    `role_name`   VARCHAR(255) NOT NULL COMMENT '角色名称（唯一约束）',
    `role_desc`   VARCHAR(255)          DEFAULT NULL COMMENT '角色描述',
    `role_status` INT                   DEFAULT 1 COMMENT '状态：1启用/0禁用',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`role_id`),
    UNIQUE KEY `uk_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 权限表
CREATE TABLE `permission` (
    `perm_id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限唯一标识',
    `perm_key`       VARCHAR(255) NOT NULL COMMENT '权限键（唯一标识符）',
    `perm_name`      VARCHAR(255)          DEFAULT NULL COMMENT '权限名称',
    `perm_type`      VARCHAR(50)           DEFAULT NULL COMMENT '权限类型',
    `parent_id`      BIGINT                DEFAULT 0 COMMENT '父权限ID',
    `display_order`  INT                   DEFAULT 0 COMMENT '显示顺序',
    `perm_status`    INT                   DEFAULT 1 COMMENT '状态：0禁用/1启用',
    `created_at`     DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`perm_id`),
    UNIQUE KEY `uk_perm_key` (`perm_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- 用户-角色关联表
CREATE TABLE `user_role` (
    `userrole_id` BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `role_id`     BIGINT   NOT NULL COMMENT '角色ID',
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`userrole_id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

-- 角色-权限关联表
CREATE TABLE `role_perm` (
    `roleperm_id` BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `role_id`     BIGINT   NOT NULL COMMENT '角色ID',
    `perm_id`     BIGINT   NOT NULL COMMENT '权限ID',
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`roleperm_id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `perm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表';

-- 部门表
CREATE TABLE `department` (
    `department_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门唯一标识',
    `department_name` VARCHAR(255) NOT NULL COMMENT '部门名称',
    PRIMARY KEY (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门信息表';

-- 用户-部门关联表
CREATE TABLE `user_department` (
    `userdepartment_id` BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `user_id`           BIGINT   NOT NULL COMMENT '用户ID',
    `department_id`     BIGINT   NOT NULL COMMENT '部门ID',
    `is_primary`        INT      DEFAULT 0 COMMENT '是否主部门',
    `created_at`        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`userdepartment_id`),
    UNIQUE KEY `uk_user_dept` (`user_id`, `department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-部门关联表';

-- ============================================================
-- 2. 物料/基础数据模块
-- ============================================================

-- 计量单位表
CREATE TABLE `unit` (
    `unit_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '单位唯一标识',
    `unit_name` VARCHAR(255) NOT NULL COMMENT '单位中文名称（法定全称）',
    `symbol`    VARCHAR(255)          DEFAULT NULL COMMENT '单位符号（区分大小写）',
    PRIMARY KEY (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计量单位表';

-- 药品剂型分类表
CREATE TABLE `dosage_form` (
    `dosage_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '剂型唯一标识',
    `dosage_name` VARCHAR(255) NOT NULL COMMENT '剂型名称（法定全称）',
    `remark`      VARCHAR(255)          DEFAULT NULL COMMENT '剂型特性备注',
    PRIMARY KEY (`dosage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品剂型分类表';

-- 物料信息表
CREATE TABLE `material` (
    `material_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '物料唯一标识符',
    `material_code`   VARCHAR(255) NOT NULL COMMENT '物料编码（唯一性约束）',
    `material_name`   VARCHAR(255) NOT NULL COMMENT '物料名称',
    `category_name`   VARCHAR(255)          DEFAULT NULL COMMENT '分类（如原料/辅料/包材等）',
    `unit_name`       VARCHAR(255)          DEFAULT NULL COMMENT '计量单位（直接存文本，如kg/张/瓶）',
    `spec`            VARCHAR(255)          DEFAULT NULL COMMENT '规格描述',
    `material_status` INT                   DEFAULT 1 COMMENT '状态：1启用/0禁用',
    `remark`          VARCHAR(255)          DEFAULT NULL COMMENT '备注信息',
    `created_by`      BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `updated_by`      BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `created_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`material_id`),
    UNIQUE KEY `uk_material_code` (`material_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料信息表';

-- ============================================================
-- 3. 生产管理模块
-- ============================================================

-- 房间表
CREATE TABLE `room_info` (
    `room_id`       INT           NOT NULL AUTO_INCREMENT COMMENT '房间ID，主键',
    `room_name`     VARCHAR(255)  NOT NULL COMMENT '房间名',
    `room_location` VARCHAR(255)           DEFAULT NULL COMMENT '房间位置',
    `area`          DECIMAL(18,2)          DEFAULT NULL COMMENT '面积（㎡）',
    `remark`        VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    `room_status`   INT                    DEFAULT 1 COMMENT '房间状态：1启用/0停用',
    `creator_id`    BIGINT                 DEFAULT NULL COMMENT '创建人ID',
    `created_time`  DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_id`    BIGINT                 DEFAULT NULL COMMENT '修改人ID',
    `updated_time`  DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间表';

-- 设备表
CREATE TABLE `equipment` (
    `equipment_id`          INT           NOT NULL AUTO_INCREMENT COMMENT '设备ID，主键',
    `equipment_name`        VARCHAR(255)  NOT NULL COMMENT '设备名称',
    `equipment_model`       VARCHAR(255)           DEFAULT NULL COMMENT '设备型号',
    `room_id`               INT                    DEFAULT NULL COMMENT '所在房间ID',
    `production_capacity`   VARCHAR(255)           DEFAULT NULL COMMENT '生产能力',
    `equipment_status`      INT                    DEFAULT 1 COMMENT '设备状态：1启用/0停用',
    `fixed_asset_code`      VARCHAR(255)           DEFAULT NULL COMMENT '固定资产编号',
    `manufacturer`          VARCHAR(255)           DEFAULT NULL COMMENT '生产厂家',
    `purchase_date`         DATE                   DEFAULT NULL COMMENT '购置时间',
    `purchase_amount`       DECIMAL(18,2)          DEFAULT NULL COMMENT '购置金额',
    `last_maintenance_date` DATE                   DEFAULT NULL COMMENT '上次维保时间',
    `remark`                VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    `creator_id`            BIGINT                 DEFAULT NULL COMMENT '创建人ID',
    `created_time`          DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_id`            BIGINT                 DEFAULT NULL COMMENT '修改人ID',
    `updated_time`          DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (`equipment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

-- 工序类型表
CREATE TABLE `process_type` (
    `process_id`          INT          NOT NULL AUTO_INCREMENT COMMENT '工序类型ID，主键',
    `process_code`        VARCHAR(255) NOT NULL COMMENT '工序类型编码',
    `process_name`        VARCHAR(255) NOT NULL COMMENT '工序类型名称',
    `process_description` VARCHAR(255)          DEFAULT NULL COMMENT '工序类型说明',
    `process_status`      INT                   DEFAULT 1 COMMENT '状态：1启用/0未启用',
    `creator_id`          BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `created_time`        DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_id`          BIGINT                DEFAULT NULL COMMENT '修改人ID',
    `updated_time`        DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (`process_id`),
    UNIQUE KEY `uk_process_code` (`process_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序类型表';

-- 生产单位信息表
CREATE TABLE `production_unit` (
    `prod_unit_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '生产单位唯一标识',
    `prod_unit_code`    VARCHAR(255) NOT NULL COMMENT '生产单位编号（唯一性约束）',
    `prod_unit_name`    VARCHAR(255) NOT NULL COMMENT '生产单位名称',
    `prod_unit_address` VARCHAR(255)          DEFAULT NULL COMMENT '生产单位地址',
    `prod_unit_manager` VARCHAR(255)          DEFAULT NULL COMMENT '负责人姓名',
    `prod_unit_phone`   VARCHAR(255)          DEFAULT NULL COMMENT '联系电话',
    `prod_unit_status`  INT                   DEFAULT 1 COMMENT '状态：0停用/1启用',
    `prod_unit_remark`  VARCHAR(255)          DEFAULT NULL COMMENT '备注信息',
    `created_by`        BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `created_time`      DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`        BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `update_time`       DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`prod_unit_id`),
    UNIQUE KEY `uk_prod_unit_code` (`prod_unit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产单位信息表';

-- 生产单位发票信息表
CREATE TABLE `prod_unit_invoice` (
    `prod_invoice_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '发票信息唯一标识',
    `prod_unit_id`      BIGINT       NOT NULL COMMENT '关联的生产单位ID',
    `prod_invoice_info` VARCHAR(255)          DEFAULT NULL COMMENT '发票信息内容',
    `created_by`        BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `created_time`      DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`        BIGINT                DEFAULT NULL COMMENT '更新人ID',
    `update_time`       DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`prod_invoice_id`),
    KEY `idx_prod_unit_id` (`prod_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产单位发票信息表';

-- 生产单位材料文件表（Base64存储）
CREATE TABLE `prod_unit_material_file` (
    `prod_material_id` BIGINT   NOT NULL AUTO_INCREMENT COMMENT '材料文件唯一标识',
    `prod_unit_id`     BIGINT   NOT NULL COMMENT '关联的生产单位ID',
    `material_type`    VARCHAR(50)        DEFAULT NULL COMMENT '材料文件类型',
    `file_name`        VARCHAR(255)       DEFAULT NULL COMMENT '文件名称',
    `file_md5`         VARCHAR(255)       DEFAULT NULL COMMENT '文件内容MD5哈希值',
    `file_size`        INT                DEFAULT NULL COMMENT '文件大小（字节）',
    `description`      VARCHAR(255)       DEFAULT NULL COMMENT '文件描述',
    `file_content`     LONGTEXT           DEFAULT NULL COMMENT '文件内容（Base64编码）',
    `created_by`       BIGINT             DEFAULT NULL COMMENT '创建人ID',
    `created_time`     DATETIME           DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`prod_material_id`),
    KEY `idx_prod_unit_id` (`prod_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产单位材料文件表';

-- 制剂信息表
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
    `unit_name`         VARCHAR(255)            DEFAULT NULL COMMENT '单位名称',
    `dosage_form`       VARCHAR(255)            DEFAULT NULL COMMENT '剂型',
    `producer`          VARCHAR(255)            DEFAULT NULL COMMENT '生产单位',
    `batch_qty`         DECIMAL(18,2)           DEFAULT NULL COMMENT '批量',
    `invoice_price`     DECIMAL(18,2)           DEFAULT NULL COMMENT '开票单价',
    `insurance_price`   DECIMAL(18,2)           DEFAULT NULL COMMENT '医保单价',
    `settlement_price`  DECIMAL(18,2)           DEFAULT NULL COMMENT '结算单价',
    `status`            INT                     DEFAULT 1 COMMENT '状态：1启用/0禁用',
    `created_by`        BIGINT                  DEFAULT NULL COMMENT '创建人ID',
    `updated_by`        BIGINT                  DEFAULT NULL COMMENT '更新人ID',
    `created_time`      DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`      DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`preparation_id`),
    UNIQUE KEY `uk_preparation_code` (`preparation_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='制剂信息表';

-- 制剂处方信息表
CREATE TABLE `preparation_formula` (
    `formula_id`        BIGINT         NOT NULL AUTO_INCREMENT COMMENT '处方明细唯一标识',
    `preparation_id`    BIGINT         NOT NULL COMMENT '制剂ID',
    `preparation_code`  VARCHAR(255)            DEFAULT NULL COMMENT '制剂编码',
    `preparation_name`  VARCHAR(255)            DEFAULT NULL COMMENT '制剂品名',
    `material_id`       BIGINT                  DEFAULT NULL COMMENT '原料ID',
    `material_code`     VARCHAR(255)            DEFAULT NULL COMMENT '原料编号',
    `material_name`     VARCHAR(255)            DEFAULT NULL COMMENT '原料名称',
    `material_category` VARCHAR(255)            DEFAULT NULL COMMENT '原料分类（原料/辅料/包材）',
    `dosage`            DECIMAL(18,2)           DEFAULT NULL COMMENT '处方量',
    `unit_id`           BIGINT                  DEFAULT NULL COMMENT '单位ID',
    `unit_name`         VARCHAR(255)            DEFAULT NULL COMMENT '单位名称（kg/张/个）',
    `created_by`        BIGINT                  DEFAULT NULL COMMENT '创建人ID',
    `updated_by`        BIGINT                  DEFAULT NULL COMMENT '更新人ID',
    `created_time`      DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`      DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`formula_id`),
    KEY `idx_preparation_id` (`preparation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='制剂处方信息表';

-- 生产计划主表
CREATE TABLE `production_plan` (
    `id`                    INT           NOT NULL AUTO_INCREMENT COMMENT '计划唯一标识',
    `plan_number`           VARCHAR(255)           DEFAULT NULL COMMENT '计划编号',
    `related_order`         VARCHAR(255)           DEFAULT NULL COMMENT '关联单号（销售订单号）',
    `preparation_code`      VARCHAR(255)           DEFAULT NULL COMMENT '制剂编码',
    `preparation_name`      VARCHAR(255)           DEFAULT NULL COMMENT '制剂名称',
    `plan_quantity`         DECIMAL(18,2)          DEFAULT NULL COMMENT '计划数量（批量）',
    `plan_type`             VARCHAR(50)            DEFAULT NULL COMMENT '生产计划类型',
    `current_status`        VARCHAR(255)           DEFAULT NULL COMMENT '当前状态',
    `current_status_date`   DATETIME               DEFAULT NULL COMMENT '当前状态时间',
    `create_time`           DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_user`           BIGINT                 DEFAULT NULL COMMENT '创建人员ID',
    `update_time`           DATETIME               DEFAULT NULL COMMENT '最后更新时间',
    `update_user`           BIGINT                 DEFAULT NULL COMMENT '最后更新人员ID',
    `remark`                VARCHAR(255)           DEFAULT NULL COMMENT '备注信息',
    `is_archived`           INT                    DEFAULT 0 COMMENT '是否已归档（0-未归档，1-已归档）',
    `unit_name`             VARCHAR(255)           DEFAULT NULL COMMENT '制剂所属单位',
    `production_unit`       VARCHAR(255)           DEFAULT NULL COMMENT '生产单位',
    `unit_price`            DECIMAL(18,2)          DEFAULT NULL COMMENT '单价',
    `finished_quantity`     DECIMAL(18,2)          DEFAULT NULL COMMENT '成品数量',
    `production_cycle`      INT                    DEFAULT NULL COMMENT '周期（天）',
    `yield_rate`            DECIMAL(18,2)          DEFAULT NULL COMMENT '得率（百分比）',
    `total_amount`          DECIMAL(18,2)          DEFAULT NULL COMMENT '总金额',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产计划主表';

-- 计划状态流水表
CREATE TABLE `plan_status_log` (
    `id`          INT          NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    `plan_id`     INT          NOT NULL COMMENT '外键，关联生产计划ID',
    `from_status` VARCHAR(255)          DEFAULT NULL COMMENT '变更前状态',
    `to_status`   VARCHAR(255)          DEFAULT NULL COMMENT '变更后状态',
    `change_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '状态变更时间',
    `operator`    BIGINT                DEFAULT NULL COMMENT '操作人员ID',
    `remark`      VARCHAR(255)          DEFAULT NULL COMMENT '变更原因或备注',
    PRIMARY KEY (`id`),
    KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划状态流水表';

-- 生产工序记录表
CREATE TABLE `production_process_record` (
    `record_id`      BIGINT         NOT NULL AUTO_INCREMENT COMMENT '记录ID，主键',
    `plan_id`        INT            NOT NULL COMMENT '生产计划ID',
    `process_name`   VARCHAR(255)            DEFAULT NULL COMMENT '工序类型名称',
    `operator_name`  VARCHAR(255)            DEFAULT NULL COMMENT '操作人姓名',
    `step_order`     INT                     DEFAULT NULL COMMENT '工序顺序',
    `workshop`       VARCHAR(255)            DEFAULT NULL COMMENT '配置室',
    `processing_qty` DECIMAL(18,2)           DEFAULT NULL COMMENT '加工数量',
    `unit_name`      VARCHAR(255)            DEFAULT NULL COMMENT '计量单位',
    `equipment`      VARCHAR(255)            DEFAULT NULL COMMENT '使用设备',
    `start_time`     DATETIME                DEFAULT NULL COMMENT '工序开始时间',
    `end_time`       DATETIME                DEFAULT NULL COMMENT '工序结束时间',
    `record_status`  INT                     DEFAULT 1 COMMENT '记录状态：1正常/0作废',
    `remark`         VARCHAR(255)            DEFAULT NULL COMMENT '备注信息',
    `creator_id`     BIGINT                  DEFAULT NULL COMMENT '创建人ID',
    `created_time`   DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater_id`     BIGINT                  DEFAULT NULL COMMENT '修改人ID',
    `updated_time`   DATETIME                DEFAULT NULL COMMENT '最后修改时间',
    PRIMARY KEY (`record_id`),
    KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产工序记录表';

-- 工单表
CREATE TABLE `work_order` (
    `work_order_id`      BIGINT         NOT NULL AUTO_INCREMENT COMMENT '工单唯一标识',
    `work_order_code`    VARCHAR(255)   NOT NULL COMMENT '工单编号（唯一性约束）',
    `work_order_name`    VARCHAR(255)            DEFAULT NULL COMMENT '工单名称',
    `preparation_id`     BIGINT                  DEFAULT NULL COMMENT '制剂ID',
    `preparation_code`   VARCHAR(255)            DEFAULT NULL COMMENT '制剂编码',
    `preparation_name`   VARCHAR(255)            DEFAULT NULL COMMENT '制剂名称',
    `batch_qty`          DECIMAL(18,2)           DEFAULT NULL COMMENT '批量',
    `producer`           VARCHAR(255)            DEFAULT NULL COMMENT '生产单位',
    `receiver`           VARCHAR(255)            DEFAULT NULL COMMENT '收货单位',
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
    `created_by`         BIGINT                  DEFAULT NULL COMMENT '创建人ID',
    `updated_by`         BIGINT                  DEFAULT NULL COMMENT '更新人ID',
    `created_time`       DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`       DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`work_order_id`),
    UNIQUE KEY `uk_work_order_code` (`work_order_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- ============================================================
-- 4. 采购管理模块
-- ============================================================

-- 采购供应商信息表
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
    `status`          VARCHAR(50)           DEFAULT NULL COMMENT '状态',
    `material_info`   VARCHAR(255)          DEFAULT NULL COMMENT '材料信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_supplier_number` (`supplier_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购供应商信息表';

-- 采购订单主表
CREATE TABLE `purchase_orders` (
    `id`                        INT           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `purchase_number`           VARCHAR(255)  NOT NULL COMMENT '采购编号',
    `warehouse`                 VARCHAR(255)           DEFAULT NULL COMMENT '仓库',
    `processing_date`           DATE                   DEFAULT NULL COMMENT '处理日期',
    `expected_delivery_date`    DATE                   DEFAULT NULL COMMENT '预计到货日期',
    `invoice_info`              VARCHAR(255)           DEFAULT NULL COMMENT '发票信息',
    `receiving_info`            VARCHAR(255)           DEFAULT NULL COMMENT '收货信息',
    `unit`                      VARCHAR(255)           DEFAULT NULL COMMENT '制剂所属单位',
    `title`                     VARCHAR(255)           DEFAULT NULL COMMENT '采购单标题',
    `prescription_multiple`     DECIMAL(18,2)          DEFAULT NULL COMMENT '处方倍数',
    `remark`                    VARCHAR(255)           DEFAULT NULL COMMENT '备注',
    `generate_production_plan`  INT                    DEFAULT 0 COMMENT '是否生成生产计划',
    `status`                    VARCHAR(50)            DEFAULT NULL COMMENT '状态',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_purchase_number` (`purchase_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单主表';

-- 采购订单明细表
CREATE TABLE `purchase_order_items` (
    `id`                 INT           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id`           INT           NOT NULL COMMENT '关联订单ID',
    `sequence_number`    INT                    DEFAULT NULL COMMENT '序号',
    `product_name`       VARCHAR(255)           DEFAULT NULL COMMENT '制剂名称',
    `raw_material_name`  VARCHAR(255)           DEFAULT NULL COMMENT '原药材品名',
    `dose`               DECIMAL(18,2)          DEFAULT NULL COMMENT '原药材剂量',
    `unit`               VARCHAR(255)           DEFAULT NULL COMMENT '单位',
    `processing_property` VARCHAR(255)          DEFAULT NULL COMMENT '加工性质',
    `stock`              DECIMAL(18,2)          DEFAULT NULL COMMENT '库存',
    `purchase_quantity`  DECIMAL(18,2)          DEFAULT NULL COMMENT '采购数量',
    `difference`         DECIMAL(18,2)          DEFAULT NULL COMMENT '差值',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单明细表';

-- ============================================================
-- 5. 库存管理模块
-- ============================================================

-- 库存表
CREATE TABLE `stock` (
    `stock_id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '库存唯一标识符',
    `prod_unit_id`      BIGINT                  DEFAULT NULL COMMENT '关联的生产单位ID',
    `item_type`         VARCHAR(50)             DEFAULT NULL COMMENT '物品类型：material物料/preparation制剂',
    `item_id`           BIGINT                  DEFAULT NULL COMMENT '关联的物品ID',
    `item_code`         VARCHAR(255)            DEFAULT NULL COMMENT '物品编码（冗余存储）',
    `item_name`         VARCHAR(255)            DEFAULT NULL COMMENT '物品名称（冗余存储）',
    `category_name`     VARCHAR(255)            DEFAULT NULL COMMENT '分类名称',
    `unit_name`         VARCHAR(255)            DEFAULT NULL COMMENT '计量单位',
    `quantity`          DECIMAL(18,2)           DEFAULT 0 COMMENT '库存数量',
    `min_quantity`      DECIMAL(18,2)           DEFAULT NULL COMMENT '最低库存预警数量',
    `max_quantity`      DECIMAL(18,2)           DEFAULT NULL COMMENT '最高库存限制数量',
    `batch_number`      VARCHAR(255)            DEFAULT NULL COMMENT '批次号',
    `production_date`   DATE                    DEFAULT NULL COMMENT '生产日期',
    `expiry_date`       DATE                    DEFAULT NULL COMMENT '有效期至',
    `storage_location`  VARCHAR(255)            DEFAULT NULL COMMENT '库位/货架号',
    `stock_status`      VARCHAR(50)             DEFAULT NULL COMMENT '库存状态',
    `remark`            VARCHAR(255)            DEFAULT NULL COMMENT '备注信息',
    `created_by`        BIGINT                  DEFAULT NULL COMMENT '创建人ID',
    `created_time`      DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`        BIGINT                  DEFAULT NULL COMMENT '更新人ID',
    `updated_time`      DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`stock_id`),
    KEY `idx_prod_unit_id` (`prod_unit_id`),
    KEY `idx_item_type_id` (`item_type`, `item_id`)
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

-- 入库主表
CREATE TABLE `stock_in` (
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
    `created_by`     BIGINT                  DEFAULT NULL COMMENT '创建人ID',
    `created_time`   DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`     BIGINT                  DEFAULT NULL COMMENT '更新人ID',
    `updated_time`   DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`in_id`),
    UNIQUE KEY `uk_in_code` (`in_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库主表';

-- 入库明细表
CREATE TABLE `stock_in_detail` (
    `in_detail_id`    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '入库明细唯一标识',
    `in_id`           BIGINT         NOT NULL COMMENT '关联入库单ID',
    `item_type`       VARCHAR(50)             DEFAULT NULL COMMENT '物品类型',
    `item_id`         BIGINT                  DEFAULT NULL COMMENT '物品ID',
    `item_code`       VARCHAR(255)            DEFAULT NULL COMMENT '物品编码',
    `item_name`       VARCHAR(255)            DEFAULT NULL COMMENT '物品名称',
    `category_name`   VARCHAR(255)            DEFAULT NULL COMMENT '分类',
    `unit_name`       VARCHAR(255)            DEFAULT NULL COMMENT '单位',
    `batch_number`    VARCHAR(255)            DEFAULT NULL COMMENT '批次号',
    `production_date` DATE                    DEFAULT NULL COMMENT '生产日期',
    `expiry_date`     DATE                    DEFAULT NULL COMMENT '有效期至',
    `quantity`        DECIMAL(18,2)           DEFAULT NULL COMMENT '入库数量',
    `unit_price`      DECIMAL(18,2)           DEFAULT NULL COMMENT '单价',
    `amount`          DECIMAL(18,2)           DEFAULT NULL COMMENT '金额',
    `storage_location` VARCHAR(255)           DEFAULT NULL COMMENT '存放位置',
    `remark`          VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`in_detail_id`),
    KEY `idx_in_id` (`in_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库明细表';

-- 出库主表
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
    `created_by`      BIGINT                  DEFAULT NULL COMMENT '创建人ID',
    `created_time`    DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      BIGINT                  DEFAULT NULL COMMENT '更新人ID',
    `updated_time`    DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`out_id`),
    UNIQUE KEY `uk_out_code` (`out_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库主表';

-- 出库明细表
CREATE TABLE `stock_out_detail` (
    `out_detail_id`  BIGINT         NOT NULL AUTO_INCREMENT COMMENT '出库明细唯一标识',
    `out_id`         BIGINT         NOT NULL COMMENT '关联出库单ID',
    `stock_id`       BIGINT                  DEFAULT NULL COMMENT '关联库存记录ID',
    `item_type`      VARCHAR(50)             DEFAULT NULL COMMENT '物品类型',
    `item_id`        BIGINT                  DEFAULT NULL COMMENT '物品ID',
    `item_code`      VARCHAR(255)            DEFAULT NULL COMMENT '物品编码',
    `item_name`      VARCHAR(255)            DEFAULT NULL COMMENT '物品名称',
    `category_name`  VARCHAR(255)            DEFAULT NULL COMMENT '分类',
    `unit_name`      VARCHAR(255)            DEFAULT NULL COMMENT '单位',
    `batch_number`   VARCHAR(255)            DEFAULT NULL COMMENT '批次号',
    `quantity`       DECIMAL(18,2)           DEFAULT NULL COMMENT '出库数量',
    `unit_price`     DECIMAL(18,2)           DEFAULT NULL COMMENT '单价',
    `amount`         DECIMAL(18,2)           DEFAULT NULL COMMENT '金额',
    `remark`         VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`out_detail_id`),
    KEY `idx_out_id` (`out_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库明细表';

-- 库存交易记录表
CREATE TABLE `stock_transaction` (
    `transaction_id`    BIGINT         NOT NULL AUTO_INCREMENT COMMENT '交易记录唯一标识',
    `stock_id`          BIGINT         NOT NULL COMMENT '关联库存ID',
    `transaction_type`  VARCHAR(50)             DEFAULT NULL COMMENT '交易类型：入库/出库/调整',
    `transaction_date`  DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
    `related_id`        BIGINT                  DEFAULT NULL COMMENT '关联单据ID',
    `related_type`      VARCHAR(50)             DEFAULT NULL COMMENT '关联单据类型',
    `quantity_before`   DECIMAL(18,2)           DEFAULT NULL COMMENT '交易前数量',
    `quantity_change`   DECIMAL(18,2)           DEFAULT NULL COMMENT '变动数量（正数增加，负数减少）',
    `quantity_after`    DECIMAL(18,2)           DEFAULT NULL COMMENT '交易后数量',
    `batch_number`      VARCHAR(255)            DEFAULT NULL COMMENT '批次号',
    `remark`            VARCHAR(255)            DEFAULT NULL COMMENT '备注',
    `created_by`        BIGINT                  DEFAULT NULL COMMENT '操作人ID',
    `created_time`      DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`transaction_id`),
    KEY `idx_stock_id` (`stock_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存交易记录表';

-- ============================================================
-- 6. 审批流程模块
-- ============================================================

-- 审批流程定义
CREATE TABLE `approval_workflow` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `workflow_name` VARCHAR(255) NOT NULL COMMENT '流程名称',
    `workflow_type` VARCHAR(255)          DEFAULT NULL COMMENT '流程类型',
    `created_at`    DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程定义';

-- 审批节点定义
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
    `created_at`         DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_workflow_id` (`workflow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批节点定义';

-- 审批实例
CREATE TABLE `approval_instance` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `workflow_id`     BIGINT       NOT NULL COMMENT '流程ID',
    `related_id`      BIGINT                DEFAULT NULL COMMENT '关联业务ID',
    `related_type`    VARCHAR(255)          DEFAULT NULL COMMENT '关联业务类型',
    `current_node_id` BIGINT                DEFAULT NULL COMMENT '当前节点ID',
    `status`          VARCHAR(50)  NOT NULL COMMENT '审批状态',
    `cancel_reason`   VARCHAR(255)          DEFAULT NULL COMMENT '作废原因',
    `cancelled_by`    BIGINT                DEFAULT NULL COMMENT '作废人ID',
    `cancelled_at`    DATETIME              DEFAULT NULL COMMENT '作废时间',
    `created_at`      DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_workflow_id` (`workflow_id`),
    KEY `idx_related` (`related_type`, `related_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批实例';

-- 审批记录
CREATE TABLE `approval_record` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `instance_id`    BIGINT       NOT NULL COMMENT '审批实例ID',
    `node_id`        BIGINT       NOT NULL COMMENT '节点ID',
    `approver_id`    BIGINT       NOT NULL COMMENT '审批人ID',
    `action`         VARCHAR(50)  NOT NULL COMMENT '审批动作',
    `target_node_id` BIGINT                DEFAULT NULL COMMENT '转交目标节点ID',
    `comment`        VARCHAR(255)          DEFAULT NULL COMMENT '审批意见',
    `created_at`     DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_instance_id` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录';

-- ============================================================
-- 7. 文件管理模块
-- ============================================================

-- 文件信息表
CREATE TABLE `file_info` (
    `file_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件唯一标识',
    `original_name`   VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `stored_name`     VARCHAR(255) NOT NULL COMMENT '存储文件名',
    `file_path`       VARCHAR(255) NOT NULL COMMENT '文件路径',
    `file_size`       BIGINT                DEFAULT NULL COMMENT '文件大小（字节）',
    `content_type`    VARCHAR(255)          DEFAULT NULL COMMENT '文件类型/内容类型',
    `file_extension`  VARCHAR(255)          DEFAULT NULL COMMENT '文件扩展名',
    `file_md5`        VARCHAR(255)          DEFAULT NULL COMMENT '文件MD5哈希值',
    `category`        VARCHAR(255)          DEFAULT NULL COMMENT '文件分类',
    `description`     VARCHAR(255)          DEFAULT NULL COMMENT '文件描述',
    `file_url`        VARCHAR(255)          DEFAULT NULL COMMENT '访问URL',
    `storage_type`    VARCHAR(50)           DEFAULT NULL COMMENT '存储类型：LOCAL/CLOUD',
    `business_id`     BIGINT                DEFAULT NULL COMMENT '关联业务ID',
    `business_type`   VARCHAR(255)          DEFAULT NULL COMMENT '关联业务类型',
    `created_by`      BIGINT                DEFAULT NULL COMMENT '创建人ID',
    `created_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`file_id`),
    KEY `idx_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';
