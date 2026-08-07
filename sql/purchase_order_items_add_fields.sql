-- ===================================
-- 采购订单明细表新增字段：物料编码、实际到货数量、单价、金额、发票号、供应商、标准处方量
-- 金额计算以实际到货数量 × 单价 为准（代码层面已同步）
-- 执行方式：在 erp_db 数据库直接执行（幂等，可重复执行）
-- ===================================

-- 0. 物料编码（search-with-details 明细回参需要）
ALTER TABLE `purchase_order_items`
    ADD COLUMN `material_code` VARCHAR(50) DEFAULT NULL COMMENT '物料编码' AFTER `material_id`;

-- 1. 标准处方量
ALTER TABLE `purchase_order_items`
    ADD COLUMN `standard_dosage` DECIMAL(18,4) DEFAULT NULL COMMENT '标准处方量' AFTER `dose`;

-- 2. 实际到货数量（供应商实际送达数量，金额以该数量为准计算）
ALTER TABLE `purchase_order_items`
    ADD COLUMN `actual_arrival_qty` DECIMAL(18,3) DEFAULT NULL COMMENT '实际到货数量（供应商实际送达数量，金额以该数量为准计算）' AFTER `purchase_quantity`;

-- 3. 单价
ALTER TABLE `purchase_order_items`
    ADD COLUMN `unit_price` DECIMAL(18,2) DEFAULT NULL COMMENT '单价' AFTER `actual_arrival_qty`;

-- 4. 金额（实际到货数量*单价）
ALTER TABLE `purchase_order_items`
    ADD COLUMN `amount` DECIMAL(18,2) DEFAULT NULL COMMENT '金额（实际到货数量*单价）' AFTER `unit_price`;

-- 5. 发票号
ALTER TABLE `purchase_order_items`
    ADD COLUMN `invoice_no` VARCHAR(100) DEFAULT NULL COMMENT '发票号' AFTER `amount`;

-- 6. 供应商
ALTER TABLE `purchase_order_items`
    ADD COLUMN `supplier` VARCHAR(200) DEFAULT NULL COMMENT '供应商' AFTER `invoice_no`;
