-- ===================================
-- 登录日志模块·数据库地基
-- 新建登录日志表 sys_login_log（按账号记录登录 IP、归属地、时间与结果）
-- 说明：幂等，可重复执行；日志只增不改，不建 is_deleted
-- ===================================

CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`      BIGINT       DEFAULT NULL COMMENT '用户ID（登录失败时可能为空）',
  `user_account` VARCHAR(64)  NOT NULL COMMENT '登录账号（失败时记录尝试的账号）',
  `user_name`    VARCHAR(64)  DEFAULT NULL COMMENT '用户姓名（成功时快照）',
  `role_ids`     VARCHAR(500) DEFAULT NULL COMMENT '角色ID快照，逗号分隔',
  `role_names`   VARCHAR(500) DEFAULT NULL COMMENT '角色名称快照，顿号分隔',
  `menu_count`   INT          NOT NULL DEFAULT 0 COMMENT '可访问菜单数',
  `menu_keys`    TEXT         DEFAULT NULL COMMENT '菜单权限键快照，逗号分隔',
  `menu_names`   TEXT         DEFAULT NULL COMMENT '菜单名称快照，顿号分隔',
  `permission_count` INT      NOT NULL DEFAULT 0 COMMENT '接口权限数',
  `permission_keys`  TEXT     DEFAULT NULL COMMENT '接口权限键快照，逗号分隔',
  `permission_names` TEXT     DEFAULT NULL COMMENT '接口权限名称快照，顿号分隔',
  `login_type`   VARCHAR(20)  NOT NULL DEFAULT 'LOGIN' COMMENT '类型：LOGIN/LOGOUT',
  `login_result` TINYINT      NOT NULL DEFAULT 1 COMMENT '结果：1成功/0失败',
  `fail_reason`  VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `login_ip`     VARCHAR(64)  DEFAULT NULL COMMENT '登录IP（IPv4/IPv6原样记录）',
  `ip_location`  VARCHAR(128) DEFAULT NULL COMMENT 'IP归属地，如 江苏省南京市/内网/未知',
  `user_agent`   VARCHAR(500) DEFAULT NULL COMMENT '客户端User-Agent',
  `login_time`   DATETIME     NOT NULL COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_login_user_time` (`user_id`, `login_time`),
  KEY `idx_login_account_time` (`user_account`, `login_time`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_login_ip` (`login_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';
