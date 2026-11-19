-- ===================================
-- 恢复「实际生产」批量保存误删的工单工序执行记录
-- 背景：POST /api/work-order-process-execution/batch 旧逻辑按“全量覆盖”处理，
--       「实际生产」只回传当前工序/批次（仅带 planId/planBatchId）时，
--       会把该工单其余工序的执行记录全部物理删除，并插入工序顺序、配置室、
--       使用设备、关键工艺参数为空的占位行。
-- 处理：
--   1) 删除上述故障操作插入的、未登记任何实际执行数据的占位行（step_order 为空）
--   2) 从 sys_data_change_log 的删除快照恢复被误删的执行记录
-- 范围：仅处理变更日志关联到 batch 接口的删除/新增，不影响正常删除的数据
-- 注意：JSON 中的 null 经 ->> 取出为字符串 'null'，所有可空字段均用 NULLIF 还原为 NULL
-- 说明：幂等，可重复执行；仅用于测试库 192.168.5.100/erp_db
-- ===================================

-- -----------------------------------
-- 1. 清理故障批量保存留下的空占位行
-- （未填写开始时间、未锁定、工序顺序为空，说明从未登记实际执行数据）
-- -----------------------------------

DELETE e
  FROM `work_order_process_execution` e
  JOIN `sys_data_change_log` d
    ON d.`table_name` = 'work_order_process_execution'
   AND d.`change_type` = 'INSERT'
   AND CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(d.`after_json`, '$.id')), 'null') AS UNSIGNED) = e.`id`
  JOIN `sys_operation_log` o
    ON o.`id` = d.`operation_log_id`
   AND o.`request_url` LIKE '%/work-order-process-execution/batch%'
 WHERE e.`is_deleted` = 0
   AND e.`step_order` IS NULL
   AND e.`start_time` IS NULL
   AND e.`end_time` IS NULL
   AND e.`locked` = 0;

-- -----------------------------------
-- 2. 从删除快照恢复被误删的执行记录
-- 条件：删除动作由 batch 接口触发；快照工序顺序非空；当前库中不存在同 ID 记录，
--       也不存在同工单 + 工序类型 + 工序顺序的有效记录，避免重复恢复
-- -----------------------------------

INSERT INTO `work_order_process_execution`
(`id`, `work_order_id`, `plan_id`, `plan_batch_id`, `batch_no`, `process_type_id`,
 `room_id`, `equipment_id`, `team_id`, `step_order`, `operator_id`, `operator_name`,
 `start_time`, `end_time`, `process_qty`, `key_process_params`, `status`, `locked`,
 `completed_time`, `remark`, `is_deleted`, `version`, `created_by`, `updated_by`,
 `created_time`, `updated_time`, `pda_submitted`, `pda_submit_by`, `pda_submit_time`,
 `pda_device_no`, `manual_time`, `manual_reason`, `client_request_id`)
SELECT CAST(NULLIF(x.`j` ->> '$.id', 'null') AS UNSIGNED),
       CAST(NULLIF(x.`j` ->> '$.work_order_id', 'null') AS UNSIGNED),
       CAST(NULLIF(x.`j` ->> '$.plan_id', 'null') AS UNSIGNED),
       CAST(NULLIF(x.`j` ->> '$.plan_batch_id', 'null') AS UNSIGNED),
       CAST(NULLIF(x.`j` ->> '$.batch_no', 'null') AS SIGNED),
       CAST(NULLIF(x.`j` ->> '$.process_type_id', 'null') AS UNSIGNED),
       CAST(NULLIF(x.`j` ->> '$.room_id', 'null') AS SIGNED),
       CAST(NULLIF(x.`j` ->> '$.equipment_id', 'null') AS SIGNED),
       CAST(NULLIF(x.`j` ->> '$.team_id', 'null') AS UNSIGNED),
       CAST(NULLIF(x.`j` ->> '$.step_order', 'null') AS SIGNED),
       CAST(NULLIF(x.`j` ->> '$.operator_id', 'null') AS UNSIGNED),
       COALESCE(NULLIF(x.`j` ->> '$.operator_name', 'null'), ''),
       STR_TO_DATE(NULLIF(x.`j` ->> '$.start_time', 'null'), '%Y-%m-%d %H:%i:%s'),
       STR_TO_DATE(NULLIF(x.`j` ->> '$.end_time', 'null'), '%Y-%m-%d %H:%i:%s'),
       CAST(NULLIF(x.`j` ->> '$.process_qty', 'null') AS DECIMAL(10, 3)),
       NULLIF(x.`j` ->> '$.key_process_params', 'null'),
       COALESCE(NULLIF(x.`j` ->> '$.status', 'null'), '待执行'),
       COALESCE(CAST(NULLIF(x.`j` ->> '$.locked', 'null') AS SIGNED), 0),
       STR_TO_DATE(NULLIF(x.`j` ->> '$.completed_time', 'null'), '%Y-%m-%d %H:%i:%s'),
       COALESCE(NULLIF(x.`j` ->> '$.remark', 'null'), ''),
       0,
       COALESCE(CAST(NULLIF(x.`j` ->> '$.version', 'null') AS SIGNED), 1),
       x.`user_id`,
       x.`user_id`,
       x.`created_time`,
       x.`created_time`,
       COALESCE(CAST(NULLIF(x.`j` ->> '$.pda_submitted', 'null') AS SIGNED), 0),
       CAST(NULLIF(x.`j` ->> '$.pda_submit_by', 'null') AS UNSIGNED),
       STR_TO_DATE(NULLIF(x.`j` ->> '$.pda_submit_time', 'null'), '%Y-%m-%d %H:%i:%s'),
       NULLIF(x.`j` ->> '$.pda_device_no', 'null'),
       COALESCE(CAST(NULLIF(x.`j` ->> '$.manual_time', 'null') AS SIGNED), 0),
       NULLIF(x.`j` ->> '$.manual_reason', 'null'),
       NULLIF(x.`j` ->> '$.client_request_id', 'null')
  FROM (
        SELECT CAST(d.`before_json` AS JSON) AS `j`,
               d.`user_id`                   AS `user_id`,
               d.`created_time`              AS `created_time`
          FROM `sys_data_change_log` d
          JOIN `sys_operation_log` o
            ON o.`id` = d.`operation_log_id`
         WHERE d.`table_name` = 'work_order_process_execution'
           AND d.`change_type` = 'DELETE'
           AND o.`request_url` LIKE '%/work-order-process-execution/batch%'
       ) x
 WHERE NULLIF(x.`j` ->> '$.step_order', 'null') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM `work_order_process_execution` e
                    WHERE e.`id` = CAST(NULLIF(x.`j` ->> '$.id', 'null') AS UNSIGNED))
   AND NOT EXISTS (SELECT 1 FROM `work_order_process_execution` e
                    WHERE e.`work_order_id` = CAST(NULLIF(x.`j` ->> '$.work_order_id', 'null') AS UNSIGNED)
                      AND e.`process_type_id` = CAST(NULLIF(x.`j` ->> '$.process_type_id', 'null') AS UNSIGNED)
                      AND e.`step_order` = CAST(NULLIF(x.`j` ->> '$.step_order', 'null') AS SIGNED)
                      AND e.`is_deleted` = 0);

-- -----------------------------------
-- 3. 执行结果核对：受影响工单的工序执行记录
-- -----------------------------------

SELECT e.`work_order_id`, w.`work_order_code`, e.`step_order`,
       pt.`process_name`, e.`room_id`, e.`equipment_id`,
       e.`key_process_params`, e.`start_time`, e.`end_time`
  FROM `work_order_process_execution` e
  JOIN `work_order` w ON w.`work_order_id` = e.`work_order_id`
  LEFT JOIN `process_type` pt ON pt.`process_id` = e.`process_type_id`
 WHERE e.`is_deleted` = 0
   AND e.`work_order_id` IN (
        SELECT DISTINCT CAST(NULLIF(d.`before_json` ->> '$.work_order_id', 'null') AS UNSIGNED)
          FROM `sys_data_change_log` d
          JOIN `sys_operation_log` o ON o.`id` = d.`operation_log_id`
         WHERE d.`table_name` = 'work_order_process_execution'
           AND d.`change_type` = 'DELETE'
           AND o.`request_url` LIKE '%/work-order-process-execution/batch%')
 ORDER BY e.`work_order_id`, e.`step_order`, e.`id`;
