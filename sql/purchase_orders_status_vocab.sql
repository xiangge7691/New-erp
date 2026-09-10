-- ===================================
-- 采购订单状态字典定义
-- 状态流转：待采购 → 运输中 → 到货初验 → 物料检验 → 已入库 / 待退货 → 已关闭
-- 说明：
--   1. 更新 status 列注释与默认值（默认初始状态为"待采购"）
--   2. 迁移旧枚举数据（draft/submitted → 待采购，received → 已入库，completed → 已关闭）
-- 执行顺序：第1步 ALTER 后再执行第2步 UPDATE（顺序无强依赖，但建议保持一致）
-- ===================================

-- 第1步：更新列注释与默认值
ALTER TABLE `purchase_orders`
MODIFY COLUMN `status` varchar(20) NOT NULL DEFAULT '待采购'
COMMENT '状态（待采购/运输中/到货初验/物料检验/已入库/待退货/已关闭）';

-- 第2步：迁移旧枚举数据为中文状态
UPDATE `purchase_orders` SET `status` = '待采购' WHERE `status` = 'draft';
UPDATE `purchase_orders` SET `status` = '待采购' WHERE `status` = 'submitted';
UPDATE `purchase_orders` SET `status` = '已入库' WHERE `status` = 'received';
UPDATE `purchase_orders` SET `status` = '已关闭' WHERE `status` = 'completed';
