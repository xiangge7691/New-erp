-- ===================================
-- 调整检验任务流程顺序（幂等）
-- 新顺序：REQUEST -> PLAN -> SAMPLING -> INSPECTION -> RETENTION -> DESTRUCTION(非必须) -> DONE
-- 适用：已执行过 create_quality_pages_tables.sql 的数据库
-- ===================================

ALTER TABLE `inspection_task`
  MODIFY COLUMN `current_step` VARCHAR(20) NOT NULL
    COMMENT '当前环节：REQUEST/PLAN/SAMPLING/INSPECTION/RETENTION/DESTRUCTION/DONE',
  MODIFY COLUMN `destruction_required` TINYINT NOT NULL DEFAULT 0
    COMMENT '是否要求销毁：1是/0否，默认非必须';

-- 1. 根据已有业务引用重算当前环节，兼容旧顺序的存量任务
UPDATE `inspection_task`
SET `current_step` = CASE
  WHEN `request_ref_id` IS NULL THEN 'REQUEST'
  WHEN `plan_ref_id` IS NULL THEN 'PLAN'
  WHEN `sampling_ref_id` IS NULL THEN 'SAMPLING'
  WHEN `inspection_ref_id` IS NULL THEN 'INSPECTION'
  WHEN `retention_required` = 1 AND `retention_ref_id` IS NULL THEN 'RETENTION'
  WHEN `destruction_required` = 1 AND `destruction_ref_id` IS NULL THEN 'DESTRUCTION'
  ELSE 'DONE'
END
WHERE `is_deleted` = 0
  AND `task_status` NOT IN ('已作废', '已归档');

-- 2. 同步任务状态
UPDATE `inspection_task`
SET `task_status` = CASE
  WHEN `current_step` = 'DONE' THEN '已完成'
  WHEN `request_ref_id` IS NULL
       AND `plan_ref_id` IS NULL
       AND `sampling_ref_id` IS NULL
       AND `inspection_ref_id` IS NULL
       AND `retention_ref_id` IS NULL
       AND `destruction_ref_id` IS NULL THEN '待开始'
  ELSE '进行中'
END
WHERE `is_deleted` = 0
  AND `task_status` NOT IN ('已作废', '已归档');

-- 3. 让六步记录状态与新顺序一致
UPDATE `inspection_task_step` s
JOIN `inspection_task` t ON t.`id` = s.`task_id`
SET s.`step_status` = CASE
  WHEN s.`step_code` = 'REQUEST' AND t.`request_ref_id` IS NOT NULL THEN '已完成'
  WHEN s.`step_code` = 'PLAN' AND t.`plan_ref_id` IS NOT NULL THEN '已完成'
  WHEN s.`step_code` = 'SAMPLING' AND t.`sampling_ref_id` IS NOT NULL THEN '已完成'
  WHEN s.`step_code` = 'INSPECTION' AND t.`inspection_ref_id` IS NOT NULL THEN '已完成'
  WHEN s.`step_code` = 'RETENTION' AND t.`retention_required` = 0 THEN '已跳过'
  WHEN s.`step_code` = 'RETENTION' AND t.`retention_ref_id` IS NOT NULL THEN '已完成'
  WHEN s.`step_code` = 'DESTRUCTION' AND t.`destruction_required` = 0 THEN '已跳过'
  WHEN s.`step_code` = 'DESTRUCTION' AND t.`destruction_ref_id` IS NOT NULL THEN '已完成'
  WHEN s.`step_code` = t.`current_step` THEN '进行中'
  ELSE '待办理'
END
WHERE t.`is_deleted` = 0
  AND t.`task_status` NOT IN ('已作废', '已归档');
