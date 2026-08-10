-- ===================================
-- 质量检验模块建表脚本
-- 表：inspection_plan（检验计划）
--     sampling_record（取样记录）
--     inspection_record（检验记录）
--     release_review（审核放行）
--     retained_sample（留样记录）
-- 说明：各模块手动创建，模块间仅通过编号字段可选引用，不建外键、不自动联动
-- 编号格式：JH/QY/JY/FX/LY-YYYYMMDD-NNN，系统自动生成、可手动修改、须唯一
-- 附件：统一使用全局文件接口 /api/files/upload-business（业务类型 QUALITY_*），表中不存储附件列
-- 执行方式：在 erp_db 数据库直接执行本脚本（可重复执行，使用 IF NOT EXISTS）
-- ===================================

-- 1. 检验计划表
CREATE TABLE IF NOT EXISTS `inspection_plan` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '检验计划唯一标识',
  `plan_code`     VARCHAR(50)  NOT NULL COMMENT '计划编号（唯一，格式JH-YYYYMMDD-NNN）',
  `plan_period`   VARCHAR(20)  NOT NULL COMMENT '计划月份/周次，如2026-07-第3周',
  `inspection_type` VARCHAR(20) NOT NULL COMMENT '检验类型：原料检验/过程检验/成品检验/环境监测',
  `object_name`   VARCHAR(100) NOT NULL COMMENT '检验对象名称（物料名称或制剂名称）',
  `batch_no`      VARCHAR(50)  DEFAULT NULL COMMENT '批号',
  `spec`          VARCHAR(100) DEFAULT NULL COMMENT '规格',
  `inspection_summary` VARCHAR(255) NOT NULL COMMENT '检验项目概要',
  `plan_time`     DATE         NOT NULL COMMENT '计划检验时间',
  `complete_time` DATE         DEFAULT NULL COMMENT '完成时间',
  `status`        VARCHAR(20)  NOT NULL DEFAULT '待检验' COMMENT '状态：待检验/检验中/已完成',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by`    BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`    BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`  DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`  DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_code` (`plan_code`),
  KEY `idx_plan_status` (`status`),
  KEY `idx_plan_time` (`plan_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验计划表';

-- 2. 取样记录表
CREATE TABLE IF NOT EXISTS `sampling_record` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '取样记录唯一标识',
  `sampling_code` VARCHAR(50)  NOT NULL COMMENT '取样编号（唯一，格式QY-YYYYMMDD-NNN）',
  `related_plan_code` VARCHAR(50) DEFAULT NULL COMMENT '关联检验计划编号（可空，可选引用）',
  `object_name`   VARCHAR(100) NOT NULL COMMENT '被检对象名称',
  `batch_no`      VARCHAR(50)  NOT NULL COMMENT '批号',
  `spec`          VARCHAR(100) DEFAULT NULL COMMENT '规格',
  `sampling_location` VARCHAR(50) NOT NULL COMMENT '取样地点：仓库/车间/洁净区等',
  `sampling_quantity` VARCHAR(50) NOT NULL COMMENT '取样量，如100g×3份',
  `sampling_count` INT          DEFAULT NULL COMMENT '取样件数',
  `sampling_method` VARCHAR(10) DEFAULT NULL COMMENT '取样方法：随机/分层/定点',
  `sampler`       VARCHAR(50)  NOT NULL COMMENT '取样人',
  `sampling_time` DATETIME     NOT NULL COMMENT '取样时间',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by`    BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`    BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`  DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`  DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sampling_code` (`sampling_code`),
  KEY `idx_sampling_time` (`sampling_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='取样记录表';

-- 3. 检验记录表
CREATE TABLE IF NOT EXISTS `inspection_record` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '检验记录唯一标识',
  `inspection_code` VARCHAR(50) NOT NULL COMMENT '检验编号（唯一，格式JY-YYYYMMDD-NNN）',
  `related_sampling_code` VARCHAR(50) DEFAULT NULL COMMENT '关联取样编号（可空，可选引用）',
  `object_name`   VARCHAR(100) NOT NULL COMMENT '被检对象名称',
  `batch_no`      VARCHAR(50)  NOT NULL COMMENT '批号',
  `spec`          VARCHAR(100) DEFAULT NULL COMMENT '规格',
  `inspection_basis` VARCHAR(255) NOT NULL COMMENT '检验依据，如中国药典2020版',
  `inspection_item` VARCHAR(1000) NOT NULL COMMENT '检验项目内容描述',
  `inspector`     VARCHAR(50)  NOT NULL COMMENT '检验人',
  `reviewer`      VARCHAR(50)  NOT NULL COMMENT '复核人',
  `start_time`    DATETIME     NOT NULL COMMENT '检验开始时间',
  `end_time`      DATETIME     NOT NULL COMMENT '检验结束时间',
  `conclusion`    VARCHAR(10)  NOT NULL COMMENT '总体结论：合格/不合格',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by`    BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`    BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`  DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`  DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inspection_code` (`inspection_code`),
  KEY `idx_inspection_conclusion` (`conclusion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验记录表';

-- 4. 审核放行表
CREATE TABLE IF NOT EXISTS `release_review` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '审核放行唯一标识',
  `release_code`  VARCHAR(50)  NOT NULL COMMENT '放行编号（唯一，格式FX-YYYYMMDD-NNN）',
  `related_inspection_code` VARCHAR(50) DEFAULT NULL COMMENT '关联检验编号（可空，可选引用）',
  `object_name`   VARCHAR(100) NOT NULL COMMENT '被检对象名称',
  `batch_no`      VARCHAR(50)  NOT NULL COMMENT '批号',
  `spec`          VARCHAR(100) DEFAULT NULL COMMENT '规格',
  `release_conclusion` VARCHAR(10) NOT NULL COMMENT '放行结论：放行/拒绝放行',
  `review_opinion` TEXT         COMMENT '审核意见（拒绝放行时必填原因）',
  `reviewer`      VARCHAR(50)  NOT NULL COMMENT '审核人',
  `review_time`   DATETIME     NOT NULL COMMENT '审核时间',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by`    BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`    BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`  DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`  DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_release_code` (`release_code`),
  KEY `idx_release_conclusion` (`release_conclusion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核放行表';

-- 5. 留样记录表
CREATE TABLE IF NOT EXISTS `retained_sample` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '留样记录唯一标识',
  `retained_code` VARCHAR(50)  NOT NULL COMMENT '留样编号（唯一，格式LY-YYYYMMDD-NNN）',
  `related_inspection_code` VARCHAR(50) DEFAULT NULL COMMENT '关联检验编号（可空，可选引用）',
  `material_name` VARCHAR(100) NOT NULL COMMENT '物料名称',
  `batch_no`      VARCHAR(50)  NOT NULL COMMENT '批号',
  `spec`          VARCHAR(100) DEFAULT NULL COMMENT '规格',
  `retained_quantity` VARCHAR(50) NOT NULL COMMENT '留样数量，如100g×2份',
  `retained_date` DATE         NOT NULL COMMENT '留样日期',
  `expiry_date`   DATE         NOT NULL COMMENT '留样期限（留样截止日期）',
  `storage_location` VARCHAR(100) NOT NULL COMMENT '存放位置（货架号/留样室）',
  `observation_record` TEXT    COMMENT '观察记录（外观、性状变化等）',
  `status`        VARCHAR(10)  NOT NULL DEFAULT '留样中' COMMENT '状态：留样中/已销毁',
  `destroy_date`  DATE         DEFAULT NULL COMMENT '销毁日期（状态=已销毁时填写）',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by`    BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`    BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`  DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`  DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_retained_code` (`retained_code`),
  KEY `idx_retained_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留样记录表';