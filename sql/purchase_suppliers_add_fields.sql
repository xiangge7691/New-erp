-- ===================================
-- 供应商表新增字段
-- ===================================

-- 1. 新增供应商类别字段
ALTER TABLE `purchase_suppliers`
ADD COLUMN `supplier_type` VARCHAR(50) DEFAULT '' COMMENT '供应商类别（定点供应商/备用供应商）' AFTER `category`;

-- 2. 新增执行质量标准字段
ALTER TABLE `purchase_suppliers`
ADD COLUMN `quality_standard` TEXT DEFAULT NULL COMMENT '执行质量标准' AFTER `supplier_type`;
