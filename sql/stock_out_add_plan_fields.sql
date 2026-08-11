-- ===================================
-- 出库单主表：新增关联生产计划字段
-- 说明：出库单需要直连生产计划，记录关联的生产计划ID与编号
--   1. plan_id     → 关联生产计划ID（production_plan.id）
--   2. plan_number → 关联生产计划编号（production_plan.plan_number）
-- 执行方式：在 erp_db 数据库直接执行（幂等，已存在字段时忽略）
-- ===================================

ALTER TABLE `stock_out`
    ADD COLUMN `plan_id` BIGINT DEFAULT NULL COMMENT '关联生产计划ID（production_plan.id）' AFTER `related_order`,
    ADD COLUMN `plan_number` VARCHAR(50) DEFAULT NULL COMMENT '关联生产计划编号（production_plan.plan_number）' AFTER `plan_id`;