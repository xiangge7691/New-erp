-- ===================================
-- 存量工单工序执行记录：按制剂工序模板自动修正设备/房间绑定
-- 背景：改造前工单工序按「设备名称」匹配并 LIMIT 1，同名设备会绑错。
-- 规则：
--   1) 仅修正「模板能唯一确定设备与房间」的执行记录
--   2) 目标设备必须存在、未删除且状态为「正常」
--   3) 仅修正状态为「待执行」的记录（已执行/执行中的历史记录不动）
--   4) 目标设备的使用时间、维保时间不得与记录时间冲突
--   5) 修正前后均输出审计明细，脚本可重复执行
-- 说明：本脚本仅用于测试库 8.134.48.54/erp_db，生产库暂不执行
-- ===================================

-- -----------------------------------
-- 1. 收集「与模板不一致」的执行记录及其目标设备
-- -----------------------------------

DROP TEMPORARY TABLE IF EXISTS `tmp_exec_equipment_diff`;
CREATE TEMPORARY TABLE `tmp_exec_equipment_diff` AS
SELECT e.`id`                AS `execution_id`,
       e.`work_order_id`     AS `work_order_id`,
       w.`work_order_code`   AS `work_order_code`,
       e.`step_order`        AS `step_order`,
       e.`status`            AS `status`,
       e.`equipment_id`      AS `old_equipment_id`,
       e.`room_id`           AS `old_room_id`,
       e.`start_time`        AS `start_time`,
       e.`end_time`          AS `end_time`,
       t.`equipment_id`      AS `target_equipment_id`,
       t.`room_id`           AS `target_room_id`,
       CAST(NULL AS CHAR(50)) AS `reason`
  FROM `work_order_process_execution` e
  JOIN `work_order` w ON w.`work_order_id` = e.`work_order_id` AND w.`is_deleted` = 0
  JOIN (
        -- 仅保留「制剂 + 工序类型 + 工序顺序」下设备/房间唯一确定的模板组
        SELECT `preparation_id`, `process_type_id`, `step_order`,
               MIN(`equipment_id`) AS `equipment_id`,
               MIN(`room_id`)      AS `room_id`
          FROM `preparation_process_template`
         WHERE `is_deleted` = 0
           AND `equipment_id` IS NOT NULL
           AND `room_id` IS NOT NULL
         GROUP BY `preparation_id`, `process_type_id`, `step_order`
        HAVING COUNT(DISTINCT `equipment_id`) = 1
           AND COUNT(DISTINCT `room_id`) = 1
       ) t
    ON t.`preparation_id` = w.`preparation_id`
   AND t.`process_type_id` = e.`process_type_id`
   AND t.`step_order` = e.`step_order`
 WHERE e.`is_deleted` = 0
   AND (e.`equipment_id` IS NULL
        OR e.`equipment_id` <> t.`equipment_id`
        OR e.`room_id` IS NULL
        OR e.`room_id` <> t.`room_id`);

-- -----------------------------------
-- 2. 标注每一条的修正判定
-- -----------------------------------

UPDATE `tmp_exec_equipment_diff` d
   SET d.`reason` = CASE
       WHEN NOT (d.`status` <=> '待执行') THEN '非待执行状态，需人工确认'
       WHEN NOT EXISTS (SELECT 1 FROM `equipment` teq
                         WHERE teq.`equipment_id` = d.`target_equipment_id`
                           AND teq.`is_deleted` = 0
                           AND teq.`equipment_status` = '正常') THEN '目标设备不可用'
       WHEN d.`start_time` IS NOT NULL AND d.`end_time` IS NOT NULL
            AND EXISTS (SELECT 1 FROM `work_order_process_execution` o
                         WHERE o.`is_deleted` = 0
                           AND o.`equipment_id` = d.`target_equipment_id`
                           AND o.`id` <> d.`execution_id`
                           AND o.`start_time` IS NOT NULL
                           AND o.`end_time` IS NOT NULL
                           AND o.`start_time` < d.`end_time`
                           AND o.`end_time` > d.`start_time`) THEN '目标设备使用时间冲突'
       WHEN d.`start_time` IS NOT NULL AND d.`end_time` IS NOT NULL
            AND EXISTS (SELECT 1 FROM `equipment_maintenance` m
                         WHERE m.`equipment_id` = d.`target_equipment_id`
                           AND m.`is_deleted` = 0
                           AND m.`maintenance_start_time` IS NOT NULL
                           AND m.`maintenance_end_time` IS NOT NULL
                           AND m.`maintenance_start_time` < d.`end_time`
                           AND m.`maintenance_end_time` > d.`start_time`) THEN '目标设备维保时间冲突'
       ELSE '可自动修正'
       END;

-- 2.1 修正前审计明细（含判定结果）
SELECT `execution_id`, `work_order_id`, `work_order_code`, `step_order`, `status`,
       `old_equipment_id`, `old_room_id`, `target_equipment_id`, `target_room_id`,
       `start_time`, `end_time`, `reason`
  FROM `tmp_exec_equipment_diff`
 ORDER BY `execution_id`;

-- -----------------------------------
-- 3. 执行自动修正
-- -----------------------------------

UPDATE `work_order_process_execution` e
  JOIN `tmp_exec_equipment_diff` d ON d.`execution_id` = e.`id`
   SET e.`equipment_id` = d.`target_equipment_id`,
       e.`room_id`      = d.`target_room_id`,
       e.`updated_time` = NOW()
 WHERE d.`reason` = '可自动修正';

-- 3.1 实际修正明细（旧值见 old_*，可据此回滚）
SELECT d.`execution_id`, d.`work_order_id`, d.`work_order_code`, d.`step_order`,
       d.`old_equipment_id`, d.`target_equipment_id`,
       d.`old_room_id`, d.`target_room_id`,
       e.`equipment_id` AS `new_equipment_id`, e.`room_id` AS `new_room_id`
  FROM `tmp_exec_equipment_diff` d
  JOIN `work_order_process_execution` e ON e.`id` = d.`execution_id`
 WHERE d.`reason` = '可自动修正'
 ORDER BY d.`execution_id`;

-- -----------------------------------
-- 4. 修正后校验
-- -----------------------------------

-- 4.1 仍与模板不一致的执行记录（应为 0 行；非 0 表示存在冲突/非待执行，需人工确认）
SELECT e.`id` AS `execution_id`, e.`work_order_id`, w.`work_order_code`, e.`step_order`,
       e.`status`, e.`equipment_id`, e.`room_id`,
       t.`equipment_id` AS `template_equipment_id`, t.`room_id` AS `template_room_id`
  FROM `work_order_process_execution` e
  JOIN `work_order` w ON w.`work_order_id` = e.`work_order_id` AND w.`is_deleted` = 0
  JOIN (
        SELECT `preparation_id`, `process_type_id`, `step_order`,
               MIN(`equipment_id`) AS `equipment_id`,
               MIN(`room_id`)      AS `room_id`
          FROM `preparation_process_template`
         WHERE `is_deleted` = 0
           AND `equipment_id` IS NOT NULL
           AND `room_id` IS NOT NULL
         GROUP BY `preparation_id`, `process_type_id`, `step_order`
        HAVING COUNT(DISTINCT `equipment_id`) = 1
           AND COUNT(DISTINCT `room_id`) = 1
       ) t
    ON t.`preparation_id` = w.`preparation_id`
   AND t.`process_type_id` = e.`process_type_id`
   AND t.`step_order` = e.`step_order`
 WHERE e.`is_deleted` = 0
   AND (e.`equipment_id` IS NULL
        OR e.`equipment_id` <> t.`equipment_id`
        OR e.`room_id` IS NULL
        OR e.`room_id` <> t.`room_id`);

-- 4.2 模板设备尚未回填、无法自动修正的执行记录数量（交业务先补齐模板）
SELECT COUNT(*) AS `template_unresolved_executions`
  FROM `work_order_process_execution` e
  JOIN `work_order` w ON w.`work_order_id` = e.`work_order_id` AND w.`is_deleted` = 0
 WHERE e.`is_deleted` = 0
   AND EXISTS (SELECT 1 FROM `preparation_process_template` t
                WHERE t.`preparation_id` = w.`preparation_id`
                  AND t.`process_type_id` = e.`process_type_id`
                  AND t.`step_order` = e.`step_order`
                  AND t.`is_deleted` = 0)
   AND NOT EXISTS (SELECT 1 FROM `preparation_process_template` t
                    WHERE t.`preparation_id` = w.`preparation_id`
                      AND t.`process_type_id` = e.`process_type_id`
                      AND t.`step_order` = e.`step_order`
                      AND t.`is_deleted` = 0
                      AND t.`equipment_id` IS NOT NULL
                      AND t.`room_id` IS NOT NULL);
