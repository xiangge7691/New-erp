-- ===================================
-- 生产排产 · 实际执行 · 人员绩效 数据库地基
-- 目标库：192.168.5.100 / erp_db
-- 说明：
--   1) 新建排产计划/批次/计划参与人、班组班次、实际参与人、绩效与系数版本表
--   2) 扩展 work_order、work_order_process_execution、process_type
--   3) 初始化绩效系数 V1，并按需回填任务计划开始时间、执行状态、操作人、实际参与人
--   4) 脚本按顺序执行一次；执行前请先备份
-- 约定：
--   - 审计字段统一使用 created_by/updated_by/created_time/updated_time
--   - 软删除统一使用 is_deleted，乐观锁统一使用 version
--   - 新表只建索引，不建外键，避免与现有软删除数据冲突
-- ===================================

-- ===================================
-- 一、班组与班次
-- ===================================

CREATE TABLE IF NOT EXISTS `production_team` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '班组ID',
  `team_code`     VARCHAR(50)  NOT NULL COMMENT '班组编码',
  `team_name`     VARCHAR(100) NOT NULL COMMENT '班组名称',
  `workshop`      VARCHAR(100) DEFAULT NULL COMMENT '所属车间',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用/1启用',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_by`    BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`    BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`  DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`  DATETIME     DEFAULT NULL COMMENT '更新时间',
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_code` (`team_code`),
  UNIQUE KEY `uk_team_name` (`team_name`),
  KEY `idx_team_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产班组';

CREATE TABLE IF NOT EXISTS `production_team_member` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `team_id`       BIGINT   NOT NULL COMMENT '班组ID',
  `personnel_id`  BIGINT   NOT NULL COMMENT '人员档案ID（personnel_file.personnel_file_id）',
  `is_primary`    TINYINT  NOT NULL DEFAULT 0 COMMENT '是否主操作：0否/1是',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_by`    BIGINT   DEFAULT NULL COMMENT '创建人ID',
  `updated_by`    BIGINT   DEFAULT NULL COMMENT '更新人ID',
  `created_time`  DATETIME DEFAULT NULL COMMENT '创建时间',
  `updated_time`  DATETIME DEFAULT NULL COMMENT '更新时间',
  `is_deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`       INT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_member` (`team_id`, `personnel_id`),
  KEY `idx_team_member_personnel` (`personnel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班组成员';

CREATE TABLE IF NOT EXISTS `production_shift` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '班次ID',
  `shift_name`    VARCHAR(50)  NOT NULL COMMENT '班次名称',
  `start_time`    TIME         NOT NULL COMMENT '开始时间',
  `end_time`      TIME         NOT NULL COMMENT '结束时间',
  `cross_day`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否跨日：0否/1是',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用/1启用',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_by`    BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`    BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`  DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`  DATETIME     DEFAULT NULL COMMENT '更新时间',
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shift_name` (`shift_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产班次';

CREATE TABLE IF NOT EXISTS `production_shift_team` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shift_id`      BIGINT   NOT NULL COMMENT '班次ID',
  `team_id`       BIGINT   NOT NULL COMMENT '班组ID',
  `created_by`    BIGINT   DEFAULT NULL COMMENT '创建人ID',
  `updated_by`    BIGINT   DEFAULT NULL COMMENT '更新人ID',
  `created_time`  DATETIME DEFAULT NULL COMMENT '创建时间',
  `updated_time`  DATETIME DEFAULT NULL COMMENT '更新时间',
  `is_deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`       INT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shift_team` (`shift_id`, `team_id`),
  KEY `idx_shift_team_team` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班次适用班组';

-- ===================================
-- 二、排产计划
-- ===================================

CREATE TABLE IF NOT EXISTS `work_order_process_plan` (
  `id`                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '计划行ID',
  `work_order_id`         BIGINT        NOT NULL COMMENT '工单ID',
  `process_type_id`       BIGINT        DEFAULT NULL COMMENT '工序类型ID',
  `template_id`           BIGINT        DEFAULT NULL COMMENT '制剂工序模板ID',
  `step_order`            INT           NOT NULL COMMENT '工序顺序',
  `plan_start_time`       DATETIME      DEFAULT NULL COMMENT '计划开始时间',
  `plan_end_time`         DATETIME      DEFAULT NULL COMMENT '计划结束时间',
  `plan_duration_minutes` INT           DEFAULT NULL COMMENT '计划用时（分钟）',
  `plan_qty`              DECIMAL(18,3) DEFAULT NULL COMMENT '计划数量',
  `room_id`               INT           DEFAULT NULL COMMENT '计划房间ID',
  `equipment_id`          INT           DEFAULT NULL COMMENT '计划设备ID',
  `team_id`               BIGINT        DEFAULT NULL COMMENT '计划班组ID',
  `status`                VARCHAR(20)   NOT NULL DEFAULT '草稿' COMMENT '状态：草稿/已保存/需重排',
  `remark`                VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  `created_by`            BIGINT        DEFAULT NULL COMMENT '创建人ID',
  `updated_by`            BIGINT        DEFAULT NULL COMMENT '更新人ID',
  `created_time`          DATETIME      DEFAULT NULL COMMENT '创建时间',
  `updated_time`          DATETIME      DEFAULT NULL COMMENT '更新时间',
  `is_deleted`            TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`               INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_work_order_step` (`work_order_id`, `step_order`),
  KEY `idx_plan_work_order` (`work_order_id`),
  KEY `idx_plan_time` (`plan_start_time`, `plan_end_time`),
  KEY `idx_plan_equipment_time` (`equipment_id`, `plan_start_time`),
  KEY `idx_plan_room_time` (`room_id`, `plan_start_time`),
  KEY `idx_plan_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单工序排产计划';

CREATE TABLE IF NOT EXISTS `work_order_process_plan_batch` (
  `id`                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `plan_id`               BIGINT        NOT NULL COMMENT '排产计划行ID',
  `batch_no`              INT           NOT NULL COMMENT '批次序号',
  `batch_name`            VARCHAR(50)   DEFAULT NULL COMMENT '批次名称',
  `plan_start_time`       DATETIME      DEFAULT NULL COMMENT '计划开始时间',
  `plan_end_time`         DATETIME      DEFAULT NULL COMMENT '计划结束时间',
  `plan_duration_minutes` INT           DEFAULT NULL COMMENT '计划用时（分钟）',
  `plan_qty`              DECIMAL(18,3) DEFAULT NULL COMMENT '计划数量',
  `room_id`               INT           DEFAULT NULL COMMENT '计划房间ID',
  `equipment_id`          INT           DEFAULT NULL COMMENT '计划设备ID',
  `team_id`               BIGINT        DEFAULT NULL COMMENT '计划班组ID',
  `status`                VARCHAR(20)   NOT NULL DEFAULT '计划中' COMMENT '状态：计划中/已完工',
  `remark`                VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  `created_by`            BIGINT        DEFAULT NULL COMMENT '创建人ID',
  `updated_by`            BIGINT        DEFAULT NULL COMMENT '更新人ID',
  `created_time`          DATETIME      DEFAULT NULL COMMENT '创建时间',
  `updated_time`          DATETIME      DEFAULT NULL COMMENT '更新时间',
  `is_deleted`            TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`               INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_batch_no` (`plan_id`, `batch_no`),
  KEY `idx_plan_batch_plan` (`plan_id`),
  KEY `idx_plan_batch_time` (`plan_start_time`, `plan_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单工序排产批次';

CREATE TABLE IF NOT EXISTS `work_order_process_plan_participant` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_id`       BIGINT   NOT NULL COMMENT '排产计划行ID',
  `batch_id`      BIGINT   DEFAULT NULL COMMENT '批次ID，为空表示工序级',
  `personnel_id`  BIGINT   NOT NULL COMMENT '人员档案ID',
  `is_primary`    TINYINT  NOT NULL DEFAULT 0 COMMENT '是否主操作：0否/1是',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_by`    BIGINT   DEFAULT NULL COMMENT '创建人ID',
  `updated_by`    BIGINT   DEFAULT NULL COMMENT '更新人ID',
  `created_time`  DATETIME DEFAULT NULL COMMENT '创建时间',
  `updated_time`  DATETIME DEFAULT NULL COMMENT '更新时间',
  `is_deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`       INT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_plan_participant_plan` (`plan_id`, `batch_id`),
  KEY `idx_plan_participant_personnel` (`personnel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排产计划参与人';

-- ===================================
-- 三、实际执行扩展
-- ===================================

ALTER TABLE `work_order_process_execution`
  ADD COLUMN `plan_id`        BIGINT  DEFAULT NULL COMMENT '排产计划行ID' AFTER `work_order_id`,
  ADD COLUMN `plan_batch_id`  BIGINT  DEFAULT NULL COMMENT '排产批次ID' AFTER `plan_id`,
  ADD COLUMN `batch_no`       INT     DEFAULT NULL COMMENT '批次序号' AFTER `plan_batch_id`,
  ADD COLUMN `team_id`        BIGINT  DEFAULT NULL COMMENT '实际班组ID' AFTER `equipment_id`,
  ADD COLUMN `locked`         TINYINT NOT NULL DEFAULT 0 COMMENT '是否已锁定：0否/1是' AFTER `status`,
  ADD COLUMN `completed_time` DATETIME DEFAULT NULL COMMENT '完工时间' AFTER `locked`,
  ADD KEY `idx_wpe_plan` (`plan_id`),
  ADD KEY `idx_wpe_plan_batch` (`plan_batch_id`);

CREATE TABLE IF NOT EXISTS `work_order_process_execution_participant` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `execution_id`   BIGINT        NOT NULL COMMENT '工序执行记录ID',
  `personnel_id`   BIGINT        NOT NULL COMMENT '人员档案ID',
  `work_hours`     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '个人工作时间（小时）',
  `completed_qty`  DECIMAL(18,3) DEFAULT NULL COMMENT '个人完成数量',
  `is_primary`     TINYINT       NOT NULL DEFAULT 0 COMMENT '是否主操作：0否/1是',
  `remark`         VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  `created_by`     BIGINT        DEFAULT NULL COMMENT '创建人ID',
  `updated_by`     BIGINT        DEFAULT NULL COMMENT '更新人ID',
  `created_time`   DATETIME      DEFAULT NULL COMMENT '创建时间',
  `updated_time`   DATETIME      DEFAULT NULL COMMENT '更新时间',
  `is_deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`        INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_execution_participant` (`execution_id`, `personnel_id`),
  KEY `idx_execution_participant_personnel` (`personnel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单工序实际参与人';

-- ===================================
-- 四、人员绩效与系数版本
-- ===================================

CREATE TABLE IF NOT EXISTS `performance_coefficient_version` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '版本ID',
  `version_no`      VARCHAR(20)  NOT NULL COMMENT '版本号，如 V1',
  `effective_time`  DATETIME     NOT NULL COMMENT '生效时间',
  `operator_id`     BIGINT       DEFAULT NULL COMMENT '发布操作人ID',
  `note`            VARCHAR(500) DEFAULT NULL COMMENT '调整说明',
  `status`          VARCHAR(20)  NOT NULL DEFAULT '生效' COMMENT '状态：生效/归档',
  `created_by`      BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`      BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`    DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`    DATETIME     DEFAULT NULL COMMENT '更新时间',
  `is_deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coefficient_version_no` (`version_no`),
  KEY `idx_coefficient_version_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绩效系数版本';

CREATE TABLE IF NOT EXISTS `performance_process_coefficient` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `version_id`       BIGINT       NOT NULL COMMENT '系数版本ID',
  `process_type_id`  BIGINT       NOT NULL COMMENT '工序类型ID',
  `coefficient`      DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '工序系数（0.5-2.0）',
  `created_by`       BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`       BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`     DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`     DATETIME     DEFAULT NULL COMMENT '更新时间',
  `is_deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`          INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version_process` (`version_id`, `process_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本内工序系数';

CREATE TABLE IF NOT EXISTS `performance_person_coefficient` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `version_id`    BIGINT       NOT NULL COMMENT '系数版本ID',
  `personnel_id`  BIGINT       NOT NULL COMMENT '人员档案ID',
  `coefficient`   DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '人员系数（0.5-2.0）',
  `created_by`    BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`    BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`  DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`  DATETIME     DEFAULT NULL COMMENT '更新时间',
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version_person` (`version_id`, `personnel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本内人员系数';

CREATE TABLE IF NOT EXISTS `performance_record` (
  `id`                        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '绩效记录ID',
  `personnel_id`              BIGINT        NOT NULL COMMENT '人员档案ID',
  `work_order_id`             BIGINT        DEFAULT NULL COMMENT '工单ID',
  `process_type_id`           BIGINT        DEFAULT NULL COMMENT '工序类型ID',
  `plan_id`                   BIGINT        DEFAULT NULL COMMENT '排产计划行ID',
  `plan_batch_id`             BIGINT        DEFAULT NULL COMMENT '排产批次ID',
  `execution_id`              BIGINT        DEFAULT NULL COMMENT '实际执行记录ID，手动记录为空',
  `batch_no`                  INT           DEFAULT NULL COMMENT '批次序号',
  `performance_date`          DATE          NOT NULL COMMENT '绩效归属日期',
  `process_actual_hours`      DECIMAL(10,2) DEFAULT NULL COMMENT '工序实际时长（小时，只读）',
  `work_hours`                DECIMAL(10,2) DEFAULT NULL COMMENT '个人工作时间（小时）',
  `performance_hours`         DECIMAL(10,2) NOT NULL COMMENT '绩效时长（小时）',
  `process_coefficient`       DECIMAL(5,2)  DEFAULT NULL COMMENT '工序系数快照',
  `person_coefficient`        DECIMAL(5,2)  DEFAULT NULL COMMENT '人员系数快照',
  `coefficient_version_id`    BIGINT        DEFAULT NULL COMMENT '系数版本ID',
  `calc_mode`                 VARCHAR(20)   NOT NULL DEFAULT '按系数计算' COMMENT '计算方式：按系数计算/直接计入',
  `performance_hours_total`   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '绩效工时',
  `completed_qty`             DECIMAL(18,3) DEFAULT NULL COMMENT '完成数量',
  `source`                    VARCHAR(20)   NOT NULL DEFAULT '系统生成' COMMENT '来源：系统生成/手动添加',
  `modified_status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '修改状态：0未修改/1已修改',
  `record_status`             TINYINT       NOT NULL DEFAULT 1 COMMENT '记录状态：1正常/0作废',
  `note`                      VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  `created_by`                BIGINT        DEFAULT NULL COMMENT '创建人ID',
  `updated_by`                BIGINT        DEFAULT NULL COMMENT '更新人ID',
  `created_time`              DATETIME      DEFAULT NULL COMMENT '创建时间',
  `updated_time`              DATETIME      DEFAULT NULL COMMENT '更新时间',
  `is_deleted`                TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`                   INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_performance_execution_person` (`execution_id`, `personnel_id`),
  KEY `idx_performance_person_date` (`personnel_id`, `performance_date`),
  KEY `idx_performance_work_order` (`work_order_id`),
  KEY `idx_performance_batch` (`plan_batch_id`),
  KEY `idx_performance_status` (`record_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员绩效记录';

-- ===================================
-- 五、现有表扩展
-- ===================================

ALTER TABLE `work_order`
  ADD COLUMN `plan_start_time` DATETIME DEFAULT NULL COMMENT '任务计划开始时间' AFTER `batch_number`,
  ADD KEY `idx_work_order_plan_start` (`plan_start_time`);

ALTER TABLE `process_type`
  ADD COLUMN `coefficient` DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '工序系数（当前生效值，0.5-2.0）' AFTER `process_status`;

-- ===================================
-- 六、初始化与回填
-- ===================================

-- 1) 任务计划开始时间：从关联生产计划的计划生产时间回填
UPDATE `work_order` w
JOIN `production_plan` p ON p.`id` = w.`plan_id` AND p.`is_deleted` = 0
SET w.`plan_start_time` = p.`plan_production_time`
WHERE w.`is_deleted` = 0
  AND w.`plan_start_time` IS NULL
  AND p.`plan_production_time` IS NOT NULL;

-- 2) 工序执行状态按实际起止重算
UPDATE `work_order_process_execution`
SET `status` = CASE
    WHEN `start_time` IS NULL THEN '待执行'
    WHEN `end_time` IS NULL THEN '执行中'
    ELSE '已完成'
  END
WHERE `is_deleted` = 0;

-- 3) 按姓名补全 operator_id（仅精确匹配人员档案姓名）
UPDATE `work_order_process_execution` e
JOIN `personnel_file` p ON p.`name` COLLATE utf8mb4_general_ci = e.`operator_name` AND p.`is_deleted` = 0
SET e.`operator_id` = p.`personnel_file_id`
WHERE e.`is_deleted` = 0
  AND e.`operator_id` IS NULL
  AND e.`operator_name` IS NOT NULL
  AND e.`operator_name` <> '';

-- 4) 为已有实际执行记录补主参与人（工作时间=实际时长，完成数量=加工数量）
INSERT INTO `work_order_process_execution_participant`
  (`execution_id`, `personnel_id`, `work_hours`, `completed_qty`, `is_primary`,
   `created_time`, `updated_time`, `is_deleted`, `version`)
SELECT e.`id`,
       e.`operator_id`,
       ROUND(TIMESTAMPDIFF(MINUTE, e.`start_time`, e.`end_time`) / 60, 2),
       e.`process_qty`,
       1,
       NOW(), NOW(), 0, 0
FROM `work_order_process_execution` e
WHERE e.`is_deleted` = 0
  AND e.`operator_id` IS NOT NULL
  AND e.`start_time` IS NOT NULL
  AND e.`end_time` IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `work_order_process_execution_participant` p
    WHERE p.`execution_id` = e.`id` AND p.`personnel_id` = e.`operator_id`
  );

-- 5) 初始化系数版本 V1
INSERT INTO `performance_coefficient_version`
  (`version_no`, `effective_time`, `operator_id`, `note`, `status`,
   `created_time`, `updated_time`, `is_deleted`, `version`)
SELECT 'V1', NOW(), NULL, '初始版本，全部工序与人员系数默认 1.0', '生效',
       NOW(), NOW(), 0, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `performance_coefficient_version` WHERE `version_no` = 'V1'
);

-- 6) V1 工序系数（每个工序类型一条）
INSERT INTO `performance_process_coefficient`
  (`version_id`, `process_type_id`, `coefficient`, `created_time`, `updated_time`, `is_deleted`, `version`)
SELECT v.`id`, p.`process_id`, 1.00, NOW(), NOW(), 0, 0
FROM `performance_coefficient_version` v
JOIN `process_type` p ON p.`is_deleted` = 0
WHERE v.`version_no` = 'V1'
  AND NOT EXISTS (
    SELECT 1 FROM `performance_process_coefficient` c
    WHERE c.`version_id` = v.`id` AND c.`process_type_id` = p.`process_id`
  );

-- 7) V1 人员系数（每个在册人员一条）
INSERT INTO `performance_person_coefficient`
  (`version_id`, `personnel_id`, `coefficient`, `created_time`, `updated_time`, `is_deleted`, `version`)
SELECT v.`id`, p.`personnel_file_id`, 1.00, NOW(), NOW(), 0, 0
FROM `performance_coefficient_version` v
JOIN `personnel_file` p ON p.`is_deleted` = 0 AND p.`status` = 1
WHERE v.`version_no` = 'V1'
  AND NOT EXISTS (
    SELECT 1 FROM `performance_person_coefficient` c
    WHERE c.`version_id` = v.`id` AND c.`personnel_id` = p.`personnel_file_id`
  );

-- ===================================
-- 七、校验查询（只读，执行后可人工核对）
-- ===================================

-- 排产唯一键检查：同一工单不应出现重复 step_order
SELECT `work_order_id`, `step_order`, COUNT(*) AS `重复数`
FROM `work_order_process_execution`
WHERE `is_deleted` = 0
GROUP BY `work_order_id`, `step_order`
HAVING COUNT(*) > 1;

-- 系数版本初始化检查
SELECT v.`version_no`, v.`status`,
       (SELECT COUNT(*) FROM `performance_process_coefficient` c WHERE c.`version_id` = v.`id`) AS `工序系数数`,
       (SELECT COUNT(*) FROM `performance_person_coefficient` c WHERE c.`version_id` = v.`id`) AS `人员系数数`
FROM `performance_coefficient_version` v;
