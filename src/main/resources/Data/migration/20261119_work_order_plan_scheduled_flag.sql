-- ===================================
-- 排产计划 / 批次新增「是否已排产」字段
-- 背景：排产明细行的“已排产 / 未排产”标识原先由后端按计划起止时间推算，
--       现改为由前端保存排产时传参决定，后端原样保存并返回。
-- 处理：
--   1) work_order_process_plan / work_order_process_plan_batch 增加 scheduled 字段
--   2) 存量数据按当前计划起止是否齐全回填一次，作为历史数据的初始标识；
--      此后保存排产以接口传参为准，后端不再自动计算
-- 说明：脚本执行一次；仅新增可空列，不影响旧版本读写（旧版本 SELECT * 不受影响）
-- ===================================

ALTER TABLE `work_order_process_plan`
  ADD COLUMN `scheduled` TINYINT(1) DEFAULT NULL COMMENT '是否已排产：1是/0否，由前端传参' AFTER `status`;

ALTER TABLE `work_order_process_plan_batch`
  ADD COLUMN `scheduled` TINYINT(1) DEFAULT NULL COMMENT '是否已排产：1是/0否，由前端传参' AFTER `status`;

-- 存量数据回填（仅初始化一次）
UPDATE `work_order_process_plan`
   SET `scheduled` = CASE
       WHEN `plan_start_time` IS NOT NULL AND `plan_end_time` IS NOT NULL THEN 1
       ELSE 0
   END
 WHERE `scheduled` IS NULL;

UPDATE `work_order_process_plan_batch`
   SET `scheduled` = CASE
       WHEN `plan_start_time` IS NOT NULL AND `plan_end_time` IS NOT NULL THEN 1
       ELSE 0
   END
 WHERE `scheduled` IS NULL;

-- 核对
SELECT `work_order_id`, `step_order`, `scheduled`, `plan_start_time`, `plan_end_time`
  FROM `work_order_process_plan`
 WHERE `is_deleted` = 0
 ORDER BY `work_order_id`, `step_order`
 LIMIT 50;
