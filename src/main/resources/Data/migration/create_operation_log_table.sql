-- ===================================
-- 操作日志模块·数据库地基
-- 新建操作日志表 sys_operation_log（记录业务写操作、审批、导入导出等）
-- 说明：幂等，可重复执行；日志只增不改，不建 is_deleted
-- ===================================

CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`        BIGINT        DEFAULT NULL COMMENT '操作人ID',
  `user_account`   VARCHAR(64)   DEFAULT NULL COMMENT '操作人账号',
  `user_name`      VARCHAR(64)   DEFAULT NULL COMMENT '操作人姓名',
  `module`         VARCHAR(64)   NOT NULL COMMENT '业务模块，如 客户管理',
  `operation_type` VARCHAR(32)   NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/APPROVE/EXPORT/IMPORT/UPLOAD/DOWNLOAD/OTHER',
  `operation_desc` VARCHAR(255)  DEFAULT NULL COMMENT '操作描述，如 新增客户',
  `business_type`  VARCHAR(64)   DEFAULT NULL COMMENT '业务对象类型，如 CUSTOMER',
  `business_id`    VARCHAR(64)   DEFAULT NULL COMMENT '业务对象ID，便于按单据追溯',
  `request_method` VARCHAR(10)   DEFAULT NULL COMMENT 'HTTP方法',
  `request_url`    VARCHAR(500)  DEFAULT NULL COMMENT '请求地址（含查询串，已截断）',
  `class_method`   VARCHAR(255)  DEFAULT NULL COMMENT '执行的方法全名',
  `request_params` TEXT          DEFAULT NULL COMMENT '请求参数摘要（脱敏 + 截断）',
  `response_code`  INT           DEFAULT NULL COMMENT '业务响应码',
  `result_status`  TINYINT       NOT NULL DEFAULT 1 COMMENT '结果：1成功/0失败',
  `error_msg`      VARCHAR(1000) DEFAULT NULL COMMENT '失败时的异常信息',
  `cost_time`      INT           DEFAULT NULL COMMENT '耗时（毫秒）',
  `client_ip`      VARCHAR(64)   DEFAULT NULL COMMENT '客户端IP（IPv4/IPv6原样记录）',
  `ip_location`    VARCHAR(128)  DEFAULT NULL COMMENT 'IP归属地',
  `user_agent`     VARCHAR(500)  DEFAULT NULL COMMENT '客户端User-Agent',
  `created_time`   DATETIME      NOT NULL COMMENT '操作时间（请求进入时间）',
  PRIMARY KEY (`id`),
  KEY `idx_op_user_time` (`user_id`, `created_time`),
  KEY `idx_op_account_time` (`user_account`, `created_time`),
  KEY `idx_op_module_time` (`module`, `created_time`),
  KEY `idx_op_type_time` (`operation_type`, `created_time`),
  KEY `idx_op_result_time` (`result_status`, `created_time`),
  KEY `idx_op_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
