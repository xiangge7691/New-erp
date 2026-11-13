-- ===================================
-- 数据变更日志模块·数据库地基
-- 新建 sys_data_change_log（记录每条数据修改前后的字段值，覆盖级联变更）
-- 说明：幂等，可重复执行；日志只增不改，不建 is_deleted
-- ===================================

CREATE TABLE IF NOT EXISTS `sys_data_change_log` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '变更日志ID',
  `trace_id`         VARCHAR(64)  NOT NULL COMMENT '请求变更TraceID',
  `operation_log_id` BIGINT       DEFAULT NULL COMMENT '关联操作日志ID',
  `table_name`       VARCHAR(64)  NOT NULL COMMENT '表名',
  `entity_class`     VARCHAR(255) DEFAULT NULL COMMENT '实体类全名',
  `business_id`      VARCHAR(64)  DEFAULT NULL COMMENT '业务记录主键值',
  `change_type`      VARCHAR(10)  NOT NULL COMMENT '变更类型：INSERT/UPDATE/DELETE',
  `change_source`    VARCHAR(10)  NOT NULL DEFAULT 'RELATED' COMMENT '来源：PRIMARY主记录/RELATED关联变更',
  `before_json`      MEDIUMTEXT   DEFAULT NULL COMMENT '修改前整行快照（脱敏、截断）',
  `after_json`       MEDIUMTEXT   DEFAULT NULL COMMENT '修改后整行快照（脱敏、截断）',
  `changed_fields`   MEDIUMTEXT   DEFAULT NULL COMMENT '字段差异JSON：[{field,label,before,after}]',
  `field_count`      INT          NOT NULL DEFAULT 0 COMMENT '发生变化的字段数',
  `user_id`          BIGINT       DEFAULT NULL COMMENT '操作人ID',
  `user_account`     VARCHAR(64)  DEFAULT NULL COMMENT '操作人账号',
  `user_name`        VARCHAR(64)  DEFAULT NULL COMMENT '操作人姓名',
  `created_time`     DATETIME     NOT NULL COMMENT '变更时间',
  PRIMARY KEY (`id`),
  KEY `idx_change_trace` (`trace_id`),
  KEY `idx_change_operation` (`operation_log_id`),
  KEY `idx_change_table_biz` (`table_name`, `business_id`),
  KEY `idx_change_table_biz_time` (`table_name`, `business_id`, `created_time`),
  KEY `idx_change_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据变更日志表';
