-- ===================================
-- 修正检验计划类型与被检品属性不一致的数据
-- 规则：生产原料→生产原料检验；中间产品→中间产品检验；成品→成品检验
-- 说明：
--   1. 同一个计划被多条任务引用时，取 id 最小的任务作为口径
--   2. 使用 NULL 安全比较，避免 inspection_type 为空的计划被漏掉
-- 执行库：8.134.48.54/erp_db（测试）、8.148.27.237/erp_db1（生产）
-- ===================================

UPDATE `inspection_plan` p
JOIN (
    SELECT t.`plan_ref_id`, MIN(t.`id`) AS `task_id`
    FROM `inspection_task` t
    WHERE t.`is_deleted` = 0 AND t.`plan_ref_id` IS NOT NULL
    GROUP BY t.`plan_ref_id`
) x ON x.`plan_ref_id` = p.`id`
JOIN `inspection_task` t ON t.`id` = x.`task_id`
SET p.`inspection_type` = CASE t.`item_category`
    WHEN '生产原料' THEN '生产原料检验'
    WHEN '中间产品' THEN '中间产品检验'
    WHEN '成品' THEN '成品检验'
    ELSE p.`inspection_type`
END
WHERE p.`is_deleted` = 0
  AND NOT (p.`inspection_type` <=> CASE t.`item_category`
    WHEN '生产原料' THEN '生产原料检验'
    WHEN '中间产品' THEN '中间产品检验'
    WHEN '成品' THEN '成品检验'
    ELSE p.`inspection_type`
END);

SELECT ROW_COUNT() AS updated_plans;
