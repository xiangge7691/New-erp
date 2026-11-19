-- ===================================
-- 工序排产 / 实际生产增加计量单位
-- 背景：工序模板已配置 unit_id，但排产计划、批次与执行记录未保存单位，
--       接口无法返回，前端数量列只能写死 kg。
-- 处理：
--   1) work_order_process_plan / work_order_process_plan_batch /
--      work_order_process_execution 增加 unit_id（快照自工序模板）
--   2) 存量数据按「制剂 + 工序类型 + 工序顺序」匹配模板回填；批次继承所属计划
-- 说明：脚本执行一次；仅新增可空列，不影响旧版本读写
-- ===================================

ALTER TABLE `work_order_process_plan`
  ADD COLUMN `unit_id` BIGINT DEFAULT NULL COMMENT '计量单位ID（快照自工序模板）' AFTER `plan_qty`;

ALTER TABLE `work_order_process_plan_batch`
  ADD COLUMN `unit_id` BIGINT DEFAULT NULL COMMENT '计量单位ID（继承所属工序计划）' AFTER `plan_qty`;

ALTER TABLE `work_order_process_execution`
  ADD COLUMN `unit_id` BIGINT DEFAULT NULL COMMENT '计量单位ID（快照自工序模板）' AFTER `process_qty`;

-- 工序计划回填
UPDATE `work_order_process_plan` p
  JOIN `work_order` w ON w.`work_order_id` = p.`work_order_id` AND w.`is_deleted` = 0
  JOIN (
        SELECT `preparation_id`, `process_type_id`, `step_order`,
               MIN(`unit_id`) AS `unit_id`
          FROM `preparation_process_template`
         WHERE `is_deleted` = 0
           AND `unit_id` IS NOT NULL
         GROUP BY `preparation_id`, `process_type_id`, `step_order`
       ) t
    ON t.`preparation_id` = w.`preparation_id`
   AND t.`process_type_id` = p.`process_type_id`
   AND t.`step_order` = p.`step_order`
   SET p.`unit_id` = t.`unit_id`
 WHERE p.`is_deleted` = 0
   AND p.`unit_id` IS NULL;

-- 执行记录回填
UPDATE `work_order_process_execution` e
  JOIN `work_order` w ON w.`work_order_id` = e.`work_order_id` AND w.`is_deleted` = 0
  JOIN (
        SELECT `preparation_id`, `process_type_id`, `step_order`,
               MIN(`unit_id`) AS `unit_id`
          FROM `preparation_process_template`
         WHERE `is_deleted` = 0
           AND `unit_id` IS NOT NULL
         GROUP BY `preparation_id`, `process_type_id`, `step_order`
       ) t
    ON t.`preparation_id` = w.`preparation_id`
   AND t.`process_type_id` = e.`process_type_id`
   AND t.`step_order` = e.`step_order`
   SET e.`unit_id` = t.`unit_id`
 WHERE e.`is_deleted` = 0
   AND e.`unit_id` IS NULL;

-- 批次继承所属工序计划
UPDATE `work_order_process_plan_batch` b
  JOIN `work_order_process_plan` p ON p.`id` = b.`plan_id`
   SET b.`unit_id` = p.`unit_id`
 WHERE b.`is_deleted` = 0
   AND b.`unit_id` IS NULL
   AND p.`unit_id` IS NOT NULL;

-- 核对：仍缺单位的业务行（应为模板本身未配置单位的工序）
SELECT 'plan' AS `biz`, COUNT(*) AS `missing_unit_rows`
  FROM `work_order_process_plan` WHERE `is_deleted` = 0 AND `unit_id` IS NULL
UNION ALL
SELECT 'batch', COUNT(*)
  FROM `work_order_process_plan_batch` WHERE `is_deleted` = 0 AND `unit_id` IS NULL
UNION ALL
SELECT 'execution', COUNT(*)
  FROM `work_order_process_execution` WHERE `is_deleted` = 0 AND `unit_id` IS NULL;
