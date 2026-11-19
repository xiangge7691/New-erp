-- ===================================
-- 恢复排产「只提交单道工序」时被误删的工序计划与参与人
-- 背景：PUT /api/scheduling/tasks/{id}/plan 旧逻辑按“全量覆盖”持久化，
--       排产页调整/拆分某一道工序时只回传该工序，后端会把未出现在请求中的
--       其余工序计划软删除，并物理删除其批次与参与人。
-- 处理：
--   1) 从变更日志识别被排产保存误软删的工序计划（同工单同工序顺序无有效计划），恢复 is_deleted=0
--   2) 从参与人新增快照恢复计划级参与人（仅恢复本次被恢复的计划，且不重复插入）
-- 范围：仅处理变更日志关联到 /api/scheduling/tasks/{id}/plan 的删除
-- 说明：幂等，可重复执行；仅用于测试库 192.168.5.100/erp_db
-- ===================================

-- -----------------------------------
-- 1. 识别被误软删的工序计划
-- -----------------------------------

DROP TEMPORARY TABLE IF EXISTS `tmp_restored_plan`;
CREATE TEMPORARY TABLE `tmp_restored_plan` (`id` BIGINT PRIMARY KEY);

INSERT INTO `tmp_restored_plan` (`id`)
SELECT DISTINCT p.`id`
  FROM `work_order_process_plan` p
  JOIN `sys_data_change_log` d
    ON d.`table_name` = 'work_order_process_plan'
   AND d.`change_type` = 'DELETE'
   AND CAST(d.`business_id` AS UNSIGNED) = p.`id`
  JOIN `sys_operation_log` o
    ON o.`id` = d.`operation_log_id`
   AND o.`request_url` LIKE '/api/scheduling/tasks/%/plan%'
 WHERE p.`is_deleted` = 1
   AND NOT EXISTS (SELECT 1 FROM `work_order_process_plan` a
                    WHERE a.`work_order_id` = p.`work_order_id`
                      AND a.`step_order` = p.`step_order`
                      AND a.`is_deleted` = 0);

-- -----------------------------------
-- 2. 恢复工序计划
-- -----------------------------------

UPDATE `work_order_process_plan` p
  JOIN `tmp_restored_plan` t ON t.`id` = p.`id`
   SET p.`is_deleted` = 0,
       p.`updated_time` = NOW();

-- -----------------------------------
-- 3. 恢复计划级参与人
-- （取该计划+人员最后一条新增快照；批次级参与人不在此恢复，避免引用已删除的批次）
-- -----------------------------------

INSERT INTO `work_order_process_plan_participant`
(`id`, `plan_id`, `batch_id`, `personnel_id`, `is_primary`, `remark`,
 `is_deleted`, `version`, `created_by`, `updated_by`, `created_time`, `updated_time`)
SELECT x.`id`, x.`plan_id`, NULL, x.`personnel_id`, x.`is_primary`, x.`remark`,
       0, 0, x.`user_id`, x.`user_id`, x.`created_time`, x.`created_time`
  FROM (
        SELECT CAST(NULLIF(d.`after_json` ->> '$.id', 'null') AS UNSIGNED)           AS `id`,
               CAST(NULLIF(d.`after_json` ->> '$.plan_id', 'null') AS UNSIGNED)       AS `plan_id`,
               CAST(NULLIF(d.`after_json` ->> '$.personnel_id', 'null') AS UNSIGNED) AS `personnel_id`,
               COALESCE(CAST(NULLIF(d.`after_json` ->> '$.is_primary', 'null') AS SIGNED), 0) AS `is_primary`,
               NULLIF(d.`after_json` ->> '$.remark', 'null')                          AS `remark`,
               d.`user_id`                                                            AS `user_id`,
               d.`created_time`                                                       AS `created_time`,
               ROW_NUMBER() OVER (
                   PARTITION BY CAST(NULLIF(d.`after_json` ->> '$.plan_id', 'null') AS UNSIGNED),
                                CAST(NULLIF(d.`after_json` ->> '$.personnel_id', 'null') AS UNSIGNED)
                   ORDER BY d.`id` DESC)                                              AS `rn`
          FROM `sys_data_change_log` d
         WHERE d.`table_name` = 'work_order_process_plan_participant'
           AND d.`change_type` = 'INSERT'
           AND NULLIF(d.`after_json` ->> '$.batch_id', 'null') IS NULL
       ) x
  JOIN `tmp_restored_plan` t ON t.`id` = x.`plan_id`
 WHERE x.`rn` = 1
   AND x.`personnel_id` IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM `work_order_process_plan_participant` q
                    WHERE q.`id` = x.`id`)
   AND NOT EXISTS (SELECT 1 FROM `work_order_process_plan_participant` q
                    WHERE q.`plan_id` = x.`plan_id`
                      AND q.`personnel_id` = x.`personnel_id`
                      AND q.`batch_id` IS NULL
                      AND q.`is_deleted` = 0);

-- -----------------------------------
-- 4. 执行结果核对
-- -----------------------------------

SELECT p.`work_order_id`, w.`work_order_code`, p.`step_order`,
       pt.`process_name`, p.`plan_qty`, p.`room_id`, p.`equipment_id`,
       p.`plan_start_time`, p.`plan_end_time`, p.`status`,
       (SELECT COUNT(*) FROM `work_order_process_plan_participant` q
         WHERE q.`plan_id` = p.`id` AND q.`is_deleted` = 0) AS `participant_count`
  FROM `work_order_process_plan` p
  JOIN `work_order` w ON w.`work_order_id` = p.`work_order_id`
  LEFT JOIN `process_type` pt ON pt.`process_id` = p.`process_type_id`
 WHERE p.`is_deleted` = 0
   AND p.`work_order_id` IN (
        SELECT DISTINCT p2.`work_order_id`
          FROM `tmp_restored_plan` t JOIN `work_order_process_plan` p2 ON p2.`id` = t.`id`)
 ORDER BY p.`work_order_id`, p.`step_order`, p.`id`;

DROP TEMPORARY TABLE IF EXISTS `tmp_restored_plan`;
