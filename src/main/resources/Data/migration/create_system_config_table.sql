-- ===================================
-- 系统配置模块·数据库地基
-- 1) 新建系统配置表 system_config（键值对，支持分组/类型/状态）
-- 说明：幂等，可重复执行
-- ===================================

CREATE TABLE IF NOT EXISTS `system_config` (
  `config_id`    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '配置唯一标识',
  `config_key`   VARCHAR(100)  NOT NULL COMMENT '配置键（唯一约束）',
  `config_value` VARCHAR(500)  DEFAULT NULL COMMENT '配置值',
  `config_name`  VARCHAR(100)  DEFAULT NULL COMMENT '配置名称（用于配置页展示）',
  `config_group` VARCHAR(50)   DEFAULT NULL COMMENT '配置分组（如 dashboard_expiry）',
  `value_type`   VARCHAR(20)   NOT NULL DEFAULT 'string' COMMENT '值类型：int/string/bool/json',
  `remark`       VARCHAR(500)  DEFAULT NULL COMMENT '配置说明',
  `status`       TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0停用/1启用',
  `created_by`   BIGINT        DEFAULT NULL COMMENT '创建人ID',
  `updated_by`   BIGINT        DEFAULT NULL COMMENT '更新人ID',
  `created_time` DATETIME      DEFAULT NULL COMMENT '创建时间',
  `updated_time` DATETIME      DEFAULT NULL COMMENT '更新时间',
  `is_deleted`   TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`      INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_config_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';
