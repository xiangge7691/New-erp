-- ===================================
-- 采购订单表：生产任务关联改为生产计划关联
-- 说明：采购订单应关联生产计划（production_plan）而非生产任务（work_order）
--   1. work_order_id   → production_plan_id（生产计划ID）
--   2. work_order_code → production_plan_code（生产计划编号）
-- 存量数据迁移：通过 work_order.plan_id 反查生产计划编号回填
-- 执行方式：在 erp_db 数据库直接执行（幂等，可重复执行）
-- ===================================

-- 1. work_order_id → production_plan_id
ALTER TABLE `purchase_orders`
    CHANGE COLUMN `work_order_id` `production_plan_id` BIGINT DEFAULT NULL COMMENT '关联生产计划ID';

-- 2. work_order_code → production_plan_code
ALTER TABLE `purchase_orders`
    CHANGE COLUMN `work_order_code` `production_plan_code` VARCHAR(50) DEFAULT NULL COMMENT '生产计划编号';

-- 3. 存量数据回填：采购订单的 work_order_id 指向生产任务，生产任务通过 plan_id 关联生产计划，
--    将生产计划ID与编号回填到采购订单（仅回填仍为空的行）
UPDATE `purchase_orders` po
    LEFT JOIN `work_order` wo ON wo.work_order_id = po.production_plan_id
    LEFT JOIN `production_plan` pp ON pp.id = wo.plan_id
SET po.production_plan_id = IFNULL(wo.plan_id, po.production_plan_id),
    po.production_plan_code = IFNULL(pp.plan_number, po.production_plan_code)
WHERE po.production_plan_id IS NOT NULL AND wo.work_order_id IS NOT NULL;
