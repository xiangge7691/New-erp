-- ===================================
-- 生产质量三页面后端表结构（幂等）
-- 1) inspection_task        检验任务（六步流程索引）
-- 2) inspection_task_step   检验任务步骤
-- 3) work_order_process_check 工单工序符合性检查
-- 4) file_requirement_config 文件必传要求配置
-- ===================================

CREATE TABLE IF NOT EXISTS `inspection_task` (
  `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_code`           VARCHAR(50)   NOT NULL COMMENT '检验任务号',
  `work_order_id`       BIGINT        NOT NULL COMMENT '工单ID',
  `work_order_code`     VARCHAR(50)   NOT NULL COMMENT '工单编号',
  `plan_id`             BIGINT        DEFAULT NULL COMMENT '生产计划ID',
  `plan_number`         VARCHAR(50)   DEFAULT NULL COMMENT '生产计划编号',
  `preparation_id`      BIGINT        DEFAULT NULL COMMENT '制剂ID',
  `preparation_code`    VARCHAR(50)   DEFAULT NULL COMMENT '制剂编码',
  `preparation_name`    VARCHAR(100)  DEFAULT NULL COMMENT '制剂名称',
  `batch_no`            VARCHAR(50)   NOT NULL DEFAULT '' COMMENT '批号，未生成批号时为空字符串',
  `item_category`       VARCHAR(20)   NOT NULL COMMENT '被检品属性：生产原料/中间产品/成品',
  `object_name`         VARCHAR(200)  DEFAULT NULL COMMENT '检验对象名称',
  `spec`                VARCHAR(100)  DEFAULT NULL COMMENT '规格',
  `current_step`        VARCHAR(20)   NOT NULL COMMENT '当前环节：PLAN/REQUEST/SAMPLING/RETENTION/INSPECTION/DESTRUCTION/DONE',
  `task_status`         VARCHAR(20)   NOT NULL COMMENT '任务状态：待开始/进行中/已完成/已跳过/已作废',
  `retention_required`  TINYINT       NOT NULL DEFAULT 1 COMMENT '是否要求留样：1是/0否',
  `destruction_required` TINYINT      NOT NULL DEFAULT 1 COMMENT '是否要求销毁：1是/0否',
  `plan_ref_id`         BIGINT        DEFAULT NULL COMMENT '检验计划ID',
  `request_ref_id`      BIGINT        DEFAULT NULL COMMENT '请检单ID',
  `sampling_ref_id`     BIGINT        DEFAULT NULL COMMENT '取样记录ID',
  `retention_ref_id`    BIGINT        DEFAULT NULL COMMENT '留样记录ID',
  `inspection_ref_id`   BIGINT        DEFAULT NULL COMMENT '检验记录ID',
  `destruction_ref_id`  BIGINT        DEFAULT NULL COMMENT '销毁记录ID',
  `start_time`          DATETIME      DEFAULT NULL COMMENT '任务开始时间',
  `complete_time`       DATETIME      DEFAULT NULL COMMENT '任务完成时间',
  `remark`              VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  `is_deleted`          TINYINT       NOT NULL DEFAULT 0 COMMENT '是否删除：0否/1是',
  `version`             INT           NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `created_by`          BIGINT        DEFAULT NULL COMMENT '创建人',
  `created_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by`          BIGINT        DEFAULT NULL COMMENT '更新人',
  `updated_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inspection_task_code` (`task_code`),
  UNIQUE KEY `uk_inspection_task_batch` (`work_order_id`, `item_category`, `batch_no`),
  KEY `idx_inspection_task_step` (`current_step`, `task_status`),
  KEY `idx_inspection_task_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验任务（六步流程索引）';

CREATE TABLE IF NOT EXISTS `inspection_task_step` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id`       BIGINT       NOT NULL COMMENT '检验任务ID',
  `step_code`     VARCHAR(20)  NOT NULL COMMENT '环节：PLAN/REQUEST/SAMPLING/RETENTION/INSPECTION/DESTRUCTION',
  `step_status`   VARCHAR(20)  NOT NULL COMMENT '状态：待办理/进行中/已完成/已跳过',
  `ref_table`     VARCHAR(50)  DEFAULT NULL COMMENT '引用表名',
  `ref_id`        BIGINT       DEFAULT NULL COMMENT '引用记录ID',
  `operator_id`   BIGINT       DEFAULT NULL COMMENT '办理人ID',
  `operator_name` VARCHAR(50)  DEFAULT NULL COMMENT '办理人姓名',
  `start_time`    DATETIME     DEFAULT NULL COMMENT '开始时间',
  `complete_time` DATETIME     DEFAULT NULL COMMENT '完成时间',
  `skip_reason`   VARCHAR(500) DEFAULT NULL COMMENT '跳过原因',
  `remark`        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除：0否/1是',
  `version`       INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `created_by`    BIGINT       DEFAULT NULL COMMENT '创建人',
  `created_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by`    BIGINT       DEFAULT NULL COMMENT '更新人',
  `updated_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_step` (`task_id`, `step_code`),
  KEY `idx_step_status` (`step_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验任务步骤';

CREATE TABLE IF NOT EXISTS `work_order_process_check` (
  `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `work_order_id`        BIGINT       NOT NULL COMMENT '工单ID',
  `execution_id`         BIGINT       DEFAULT NULL COMMENT '工单工序执行ID',
  `process_type_id`      BIGINT       DEFAULT NULL COMMENT '工序类型ID',
  `process_name`         VARCHAR(100) NOT NULL COMMENT '工序名称',
  `requirement_snapshot` TEXT         DEFAULT NULL COMMENT '检查时点的要求快照',
  `check_result`         VARCHAR(20)  NOT NULL COMMENT '结果：符合/不符合/待查',
  `inspector_id`         BIGINT       DEFAULT NULL COMMENT '工艺查证人ID',
  `inspector_name`       VARCHAR(50)  DEFAULT NULL COMMENT '工艺查证人姓名',
  `check_date`           DATE         DEFAULT NULL COMMENT '查证日期',
  `remark`               VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `is_deleted`           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除：0否/1是',
  `version`              INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `created_by`           BIGINT       DEFAULT NULL COMMENT '创建人',
  `created_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by`           BIGINT       DEFAULT NULL COMMENT '更新人',
  `updated_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_process_check_wo` (`work_order_id`),
  KEY `idx_process_check_exec` (`execution_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单工序符合性检查';

CREATE TABLE IF NOT EXISTS `file_requirement_config` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `scope`        VARCHAR(20)  NOT NULL COMMENT '范围：GLOBAL/PREPARATION/ITEM_CATEGORY',
  `scope_key`    VARCHAR(100) NOT NULL DEFAULT '' COMMENT '范围值：品种编码/被检品属性',
  `biz_type`     VARCHAR(50)  NOT NULL COMMENT '业务类型',
  `file_kind`    VARCHAR(50)  NOT NULL COMMENT '文件槽位',
  `requirement`  VARCHAR(20)  NOT NULL COMMENT '要求：REQUIRED/CONDITIONAL/OPTIONAL',
  `stage`        VARCHAR(50)  DEFAULT NULL COMMENT '环节',
  `display_name` VARCHAR(100) DEFAULT NULL COMMENT '显示名称',
  `remark`       VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `is_deleted`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除：0否/1是',
  `version`      INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `created_by`   BIGINT       DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by`   BIGINT       DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_req` (`scope`, `scope_key`, `biz_type`, `file_kind`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件必传要求配置';

-- ===================================
-- 初始化文件要求（按已确认清单）
-- ===================================
INSERT INTO `file_requirement_config`
  (`scope`, `scope_key`, `biz_type`, `file_kind`, `requirement`, `stage`, `display_name`, `remark`)
VALUES
  ('GLOBAL','','PREPARATION','PROCESS_SPEC','REQUIRED','品种资料','工艺规程',NULL),
  ('GLOBAL','','PREPARATION','QUALITY_STANDARD','REQUIRED','品种资料','质量标准',NULL),
  ('GLOBAL','','PREPARATION','BATCH_RECORD_TEMPLATE','REQUIRED','品种资料','批记录模板',NULL),
  ('GLOBAL','','PREPARATION','VERIFICATION_PLAN','CONDITIONAL','品种资料','验证方案','首次/变更/再验证'),
  ('GLOBAL','','PREPARATION','PROCESS_FLOWCHART','OPTIONAL','品种资料','工艺流程图','可空缺'),
  ('GLOBAL','','PREPARATION_PROCESS_TEMPLATE','DEFAULT','OPTIONAL','品种资料','工序模板','可空缺'),
  ('GLOBAL','','PRODUCTION_PLAN','DEFAULT','CONDITIONAL','计划','生产计划附件','计划下达/变更时'),
  ('GLOBAL','','PURCHASE_ORDER','DEFAULT','CONDITIONAL','采购','采购订单附件','采购业务发生时'),
  ('GLOBAL','','ACCEPTANCE_ORDER','WAYBILL','CONDITIONAL','验收','随货清单','随货时'),
  ('GLOBAL','','ACCEPTANCE_ORDER','INSPECTION_REPORT','REQUIRED','验收','验收检验报告',NULL),
  ('GLOBAL','','ACCEPTANCE_ORDER','INVOICE','OPTIONAL','验收','发票','财务另行归档'),
  ('GLOBAL','','WORK_ORDER','BATCH_RECORD','REQUIRED','工单','批生产记录',NULL),
  ('GLOBAL','','WORK_ORDER','OTHER','OPTIONAL','工单','其他文件','可空缺'),
  ('GLOBAL','','WORK_ORDER_PROCESS_EXECUTION','PROCESS_ATTACHMENT','OPTIONAL','工序','工序附件','已确认不必传'),
  ('GLOBAL','','STOCK_OUT_PRODUCTION','DEFAULT','OPTIONAL','领料','领料单附件','前端未接入'),
  ('GLOBAL','','STOCK_IN_PRODUCT','DEFAULT','OPTIONAL','入库','成品入库单附件','前端未接入'),
  ('GLOBAL','','STOCK_OUT_SALES','DEFAULT','OPTIONAL','出库','成品出库单附件','前端未接入'),
  ('GLOBAL','','INSPECTION_REQUEST','DEFAULT','CONDITIONAL','检验','请检单附件','请检时'),
  ('GLOBAL','','SAMPLING_RECORD','DEFAULT','CONDITIONAL','检验','取样记录附件','取样时'),
  ('GLOBAL','','INSPECTION_RECORD','DEFAULT','REQUIRED','检验','检验记录/图谱','成品检验'),
  ('GLOBAL','','RELEASE_REVIEW','DEFAULT','REQUIRED','放行','放行审核附件','放行批次'),
  ('GLOBAL','','DEVIATION_RECORD','DEFAULT','OPTIONAL','偏差','偏差处理/CAPA','已确认不必传'),
  ('GLOBAL','','DESTRUCTION_RECORD','DEFAULT','OPTIONAL','销毁','销毁记录附件','已确认不必传'),
  ('GLOBAL','','RETAINED_SAMPLE','DEFAULT','OPTIONAL','留样','留样记录附件','已确认不必传'),
  ('GLOBAL','','APPROVAL_INSTANCE','DEFAULT','OPTIONAL','审批','审批附件','已确认不必传'),
  ('GLOBAL','','DOSAGE_FORM','BATCH_RECORD_TEMPLATE','OPTIONAL','剂型','剂型批记录模板','已确认不必传'),
  ('GLOBAL','','DOSAGE_FORM','VERIFICATION_TEMPLATE','OPTIONAL','剂型','剂型验证模板','已确认不必传'),
  ('GLOBAL','','DOSAGE_FORM','INSPECTION_TEMPLATE','OPTIONAL','剂型','剂型检验模板','已确认不必传'),
  ('GLOBAL','','PRODUCTION_UNIT','DEFAULT','OPTIONAL','生产单位','仓库材料文件','已确认不必传'),
  ('GLOBAL','','EQUIPMENT','DEFAULT','OPTIONAL','设备','设备信息附件','已确认不必传'),
  ('GLOBAL','','EQUIPMENT_MAINTENANCE','DEFAULT','OPTIONAL','设备','维保附件','已确认不必传'),
  ('GLOBAL','','ROOM_INFO','DISINFECTION','OPTIONAL','车间环境','消毒记录','已确认不必传'),
  ('GLOBAL','','ROOM_INFO','CLEAN_INSPECTION','OPTIONAL','车间环境','洁净检测','已确认不必传'),
  ('GLOBAL','','ROOM_INFO','CLEANING_RECORD','OPTIONAL','车间环境','清洁记录','已确认不必传'),
  ('GLOBAL','','ENERGY_RECORD','DEFAULT','OPTIONAL','能耗','能耗记录附件','已确认不必传')
ON DUPLICATE KEY UPDATE
  `requirement` = VALUES(`requirement`),
  `stage` = VALUES(`stage`),
  `display_name` = VALUES(`display_name`),
  `remark` = VALUES(`remark`),
  `updated_time` = CURRENT_TIMESTAMP;
