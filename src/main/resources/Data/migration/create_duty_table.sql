-- ===================================
-- 职务信息模块·数据库地基
-- 1) 新建职务信息表 duty（职务编码/职务名称/职务描述/状态/排序）
-- 说明：全部幂等，可重复执行
-- ===================================

CREATE TABLE IF NOT EXISTS `duty` (
  `duty_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '职务唯一标识',
  `duty_code`   VARCHAR(50)  NOT NULL COMMENT '职务编码（唯一约束）',
  `duty_name`   VARCHAR(100) NOT NULL COMMENT '职务名称',
  `duty_desc`   VARCHAR(500) DEFAULT NULL COMMENT '职务描述',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用/1启用',
  `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_by`  BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`  BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time` DATETIME    DEFAULT NULL COMMENT '创建时间',
  `updated_time` DATETIME    DEFAULT NULL COMMENT '更新时间',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`duty_id`),
  UNIQUE KEY `uk_duty_code` (`duty_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职务信息表';