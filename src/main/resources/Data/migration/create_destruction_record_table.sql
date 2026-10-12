-- ===================================
-- 销毁记录模块·数据库地基
-- 1) 新建销毁记录主表 destruction_record（单号/记录类型/合计数量/销毁原因/方法/地点/四方签字文本/作废状态）
-- 2) 新建销毁记录明细表 destruction_record_detail（品名/规格/批号/数量/留样ID引用）
-- 说明：全部幂等，可重复执行
-- 作废语义：主表无物理/逻辑删除接口，仅 status=作废（status+void_time+void_remark），作废后禁改
-- 明细重建语义：更新时按 destruction_id 物理删除旧明细再整单重插
-- ===================================

CREATE TABLE IF NOT EXISTS `destruction_record` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '销毁记录唯一标识',
  `destruction_code` VARCHAR(32)  NOT NULL COMMENT '销毁单号（唯一约束，BPXH-YYYYMMDD-NNN/LYXH-YYYYMMDD-NNN，系统自动生成）',
  `record_type`      VARCHAR(20)  NOT NULL COMMENT '记录类型：不合格品销毁/留样及稳定性考察产品销毁',
  `total_quantity`   DECIMAL(18,3) NOT NULL DEFAULT 0 COMMENT '销毁合计数量（服务端按明细求和）',
  `destroy_reason`   VARCHAR(1000) DEFAULT NULL COMMENT '销毁原因',
  `destroy_method`   VARCHAR(1000) DEFAULT NULL COMMENT '销毁方法',
  `destroy_location` VARCHAR(200) DEFAULT NULL COMMENT '销毁地点',
  `applicant_name`   VARCHAR(50)  DEFAULT NULL COMMENT '申请人姓名（存姓名文本）',
  `apply_date`       DATE         DEFAULT NULL COMMENT '申请日期',
  `approver_name`    VARCHAR(50)  DEFAULT NULL COMMENT '批准人姓名（存姓名文本）',
  `approve_date`     DATE         DEFAULT NULL COMMENT '批准日期',
  `destroyer_name`   VARCHAR(50)  DEFAULT NULL COMMENT '销毁人姓名（存姓名文本）',
  `destroy_date`     DATE         DEFAULT NULL COMMENT '销毁日期',
  `supervisor_name`  VARCHAR(50)  DEFAULT NULL COMMENT '监销人姓名（存姓名文本）',
  `supervise_date`   DATE         DEFAULT NULL COMMENT '监销日期',
  `status`           VARCHAR(20)  NOT NULL DEFAULT '正常' COMMENT '状态：正常/作废',
  `void_time`        DATETIME     DEFAULT NULL COMMENT '作废时间（status=作废时填写）',
  `void_remark`      VARCHAR(500) DEFAULT NULL COMMENT '作废原因（status=作废时填写）',
  `remark`           VARCHAR(1000) DEFAULT NULL COMMENT '备注',
  `created_by`       BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`       BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`     DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`     DATETIME     DEFAULT NULL COMMENT '更新时间',
  `is_deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是（保留字段，业务不走删除）',
  `version`          INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_destruction_code` (`destruction_code`),
  KEY `idx_destruction_record_type` (`record_type`),
  KEY `idx_destruction_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销毁记录主表';

CREATE TABLE IF NOT EXISTS `destruction_record_detail` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '销毁明细唯一标识',
  `destruction_id`    BIGINT       NOT NULL COMMENT '关联销毁记录主表ID',
  `material_name`     VARCHAR(100) NOT NULL COMMENT '品名/物料名称',
  `spec`              VARCHAR(100) DEFAULT NULL COMMENT '规格',
  `batch_no`          VARCHAR(50)  DEFAULT NULL COMMENT '批号',
  `quantity`          DECIMAL(18,3) DEFAULT 0 COMMENT '数量',
  `retained_sample_id` BIGINT      DEFAULT NULL COMMENT '关联留样记录ID（可空，仅作选人回填与追溯引用，不联动留样状态）',
  `created_by`        BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`        BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`      DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`      DATETIME     DEFAULT NULL COMMENT '更新时间',
  `is_deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是（保留字段，重建时物理删除）',
  `version`           INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_destruction_detail_destruction_id` (`destruction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销毁记录明细表';
