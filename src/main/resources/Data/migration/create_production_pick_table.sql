-- ===================================
-- 生产领料模块建表脚本（幂等）
-- ===================================
-- 说明：
-- 1) production_pick 为生产领料主表，记录关联生产任务、用途、领料人/审核人/发料人等信息；
-- 2) production_pick_detail 为生产领料明细表，记录物料、请领数量、实发数量、实发批次等；
-- 3) 领料单发料时生成“生产领料出库”草稿出库单，仓库确认后由现有出库逻辑扣减库存。

CREATE TABLE IF NOT EXISTS `production_pick` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pick_code`          VARCHAR(50)   NOT NULL COMMENT '领料单号（SCLL-YYYYMMDD-NNN）',
  `work_order_id`      BIGINT        NOT NULL COMMENT '关联生产任务ID',
  `work_order_code`    VARCHAR(50)   NOT NULL COMMENT '关联生产任务号',
  `work_order_name`    VARCHAR(100)  DEFAULT NULL COMMENT '关联生产任务名',
  `plan_id`            BIGINT        DEFAULT NULL COMMENT '关联生产计划ID',
  `plan_number`        VARCHAR(50)   DEFAULT NULL COMMENT '关联生产计划编号',
  `preparation_id`     BIGINT        DEFAULT NULL COMMENT '制剂ID',
  `preparation_code`   VARCHAR(50)   DEFAULT NULL COMMENT '制剂编码',
  `preparation_name`   VARCHAR(100)  DEFAULT NULL COMMENT '制剂名称',
  `batch_number`       VARCHAR(50)   DEFAULT NULL COMMENT '生产批号',
  `workshop`           VARCHAR(100)  DEFAULT NULL COMMENT '领料车间',
  `pick_time`          DATETIME      NOT NULL COMMENT '领料时间',
  `purpose`            VARCHAR(200)  DEFAULT NULL COMMENT '用途',
  `applicant`          VARCHAR(50)   NOT NULL COMMENT '领料人',
  `reviewer`           VARCHAR(50)   DEFAULT NULL COMMENT '审核人',
  `reviewer_time`      DATETIME      DEFAULT NULL COMMENT '审核时间',
  `issuer`             VARCHAR(50)   DEFAULT NULL COMMENT '发料人',
  `issue_time`         DATETIME      DEFAULT NULL COMMENT '发料时间',
  `status`             VARCHAR(20)   NOT NULL DEFAULT '待发料' COMMENT '状态：待发料/已发料/已作废',
  `stock_out_id`       BIGINT        DEFAULT NULL COMMENT '联动生成的出库单ID',
  `stock_out_code`     VARCHAR(50)   DEFAULT NULL COMMENT '联动生成的出库单号',
  `multiplier`         DECIMAL(10,4) DEFAULT NULL COMMENT '处方倍数',
  `remark`             VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  `is_deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已删除',
  `version`            INT           NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `created_by`         BIGINT        DEFAULT NULL COMMENT '创建人ID',
  `updated_by`         BIGINT        DEFAULT NULL COMMENT '更新人ID',
  `created_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_production_pick_code` (`pick_code`),
  KEY `idx_production_pick_work_order` (`work_order_id`),
  KEY `idx_production_pick_status` (`status`),
  KEY `idx_production_pick_time` (`pick_time`),
  KEY `idx_production_pick_stock_out` (`stock_out_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产领料单主表';

CREATE TABLE IF NOT EXISTS `production_pick_detail` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pick_id`        BIGINT        NOT NULL COMMENT '生产领料单ID',
  `seq`            INT           NOT NULL COMMENT '明细行序号',
  `material_id`    BIGINT        DEFAULT NULL COMMENT '物料ID',
  `material_code`  VARCHAR(50)   NOT NULL COMMENT '物料编码',
  `material_name`  VARCHAR(200)  NOT NULL COMMENT '物料名称',
  `category_name`  VARCHAR(50)   DEFAULT NULL COMMENT '物料分类（原料/辅料/包材）',
  `unit_name`      VARCHAR(20)   NOT NULL COMMENT '计量单位',
  `apply_qty`      DECIMAL(14,4) NOT NULL COMMENT '请领数量',
  `actual_qty`     DECIMAL(14,4) DEFAULT NULL COMMENT '实发数量',
  `stock_id`       BIGINT        DEFAULT NULL COMMENT '实发主库存批次ID',
  `batch_number`   VARCHAR(200)  DEFAULT NULL COMMENT '实发批次（多批次逗号分隔）',
  `unit_price`     DECIMAL(18,4) DEFAULT NULL COMMENT '发料单价（加权平均/展示用）',
  `remark`         VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  `is_deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已删除',
  `version`        INT           NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_production_pick_detail_pick` (`pick_id`),
  KEY `idx_production_pick_detail_material` (`material_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产领料单明细表';
