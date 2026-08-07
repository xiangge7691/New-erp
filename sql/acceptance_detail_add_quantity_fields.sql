-- ===================================
-- 验收明细表新增字段：实际到货数量、入库数量
-- 金额计算以实际到货数量为准（代码层面已同步）
-- 执行方式：在 erp_db 数据库直接执行（幂等，可重复执行）
-- ===================================

-- 1. 实际到货数量（供应商实际送达数量，金额以该数量为准计算）
ALTER TABLE `acceptance_detail`
    ADD COLUMN `actual_arrival_qty` DECIMAL(18,3) DEFAULT NULL COMMENT '实际到货数量（供应商实际送达数量，金额以该数量为准计算）' AFTER `quantity`;

-- 2. 入库数量（检验合格后实际入库数量）
ALTER TABLE `acceptance_detail`
    ADD COLUMN `inbound_qty` DECIMAL(18,3) DEFAULT NULL COMMENT '入库数量（检验合格后实际入库数量）' AFTER `actual_arrival_qty`;
