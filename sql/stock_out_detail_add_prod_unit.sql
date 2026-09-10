-- ===================================
-- 出库明细表：新增出库仓库字段
-- 说明：支持一张出库单的明细从不同仓库（生产单位）出库，
--   明细行记录各自的出库仓库 prod_unit_id（stock.prod_unit_id）
-- 执行方式：在 erp_db 数据库直接执行（幂等，已存在字段时忽略）
-- ===================================

ALTER TABLE `stock_out_detail`
    ADD COLUMN `prod_unit_id` BIGINT DEFAULT NULL COMMENT '出库仓库（生产单位ID，明细可跨仓库）' AFTER `out_id`;

-- 历史明细回填：根据明细关联的库存批次（stock_id）回填出库仓库
UPDATE `stock_out_detail` d
LEFT JOIN `stock` s ON s.stock_id = d.stock_id
SET d.prod_unit_id = s.prod_unit_id
WHERE d.prod_unit_id IS NULL;
