-- ===================================
-- 偏差记录模块·数据库地基
-- 1) 新建偏差记录表 deviation_record（偏差编号/标题/生产任务快照/发生时间/工序/偏差描述/处理措施/处理结果/处理人）
-- 说明：全部幂等，可重复执行
-- ===================================

CREATE TABLE IF NOT EXISTS `deviation_record` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '偏差记录唯一标识',
  `deviation_code`   VARCHAR(32)  NOT NULL COMMENT '偏差编号（唯一约束，格式PC-YYYYMMDD-NNN，系统自动生成）',
  `deviation_title`  VARCHAR(200) NOT NULL COMMENT '偏差标题',
  `work_order_id`    BIGINT       DEFAULT NULL COMMENT '关联生产任务ID（可空，仅作追溯引用）',
  `work_order_code`  VARCHAR(50)  DEFAULT NULL COMMENT '生产任务编号（冗余快照，便于列表展示）',
  `preparation_name` VARCHAR(100) DEFAULT NULL COMMENT '制剂名称（冗余快照）',
  `batch_number`     VARCHAR(50)  DEFAULT NULL COMMENT '批号（冗余快照）',
  `occur_time`       DATETIME     NOT NULL COMMENT '实际发生时间',
  `process_name`     VARCHAR(100) DEFAULT NULL COMMENT '发生工序',
  `description`      TEXT         NOT NULL COMMENT '偏差描述',
  `action_desc`      TEXT         DEFAULT NULL COMMENT '处理措施',
  `result_desc`      TEXT         DEFAULT NULL COMMENT '处理结果',
  `handler_name`     VARCHAR(100) DEFAULT NULL COMMENT '处理人姓名（存姓名文本）',
  `handle_time`      DATETIME     DEFAULT NULL COMMENT '处理时间',
  `remark`           VARCHAR(1000) DEFAULT NULL COMMENT '备注',
  `created_by`       BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`       BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`     DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`     DATETIME     DEFAULT NULL COMMENT '更新时间',
  `is_deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`          INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_deviation_code` (`deviation_code`),
  KEY `idx_deviation_work_order_id` (`work_order_id`),
  KEY `idx_deviation_occur_time` (`occur_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='偏差记录表';
