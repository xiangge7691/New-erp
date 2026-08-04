-- ===================================
-- 采购模块新增「期望到货日期」字段
-- 表：purchase_plan（采购计划）、purchase_orders（采购订单）
-- 说明：期望到货日期（desired_delivery_date）为采购方期望的到货时间，
--       与现有预计到货日期（expected_delivery_date）区分
-- ===================================

ALTER TABLE `purchase_plan`
    ADD COLUMN `desired_delivery_date` date NULL COMMENT '期望到货日期（采购方期望的到货时间）' AFTER `expected_delivery_date`;

ALTER TABLE `purchase_orders`
    ADD COLUMN `desired_delivery_date` date NULL COMMENT '期望到货日期（采购方期望的到货时间）' AFTER `expected_delivery_date`;
