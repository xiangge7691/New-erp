-- ===================================
-- 采购订单模块修复脚本
-- 说明：
--   1. 触发器 trg_auto_generate_purchase_number 的 definer 为 root@%（该账号不存在），
--      导致所有对 purchase_orders 的 INSERT 报错：
--      "The user specified as a definer ('root'@'%') does not exist"
--   2. 编号生成逻辑已迁移到后端 PurchaseOrdersServiceImpl.generateOrderNumber()
--      （CG + yyyyMMdd + 4位流水号，查询时绕过软删除过滤，含并发重试），
--      因此本脚本只需删除触发器。
-- 执行顺序：先执行第2步删除触发器，第1步仅其他环境需要时执行。
-- ===================================

-- 第1步：status 列由 enum 改为 varchar(20)（远程库已执行过，其他环境如需同步可执行）
ALTER TABLE `purchase_orders`
MODIFY COLUMN `status` varchar(20) NOT NULL DEFAULT 'draft' COMMENT '状态';

-- 第2步：删除采购订单编号生成触发器（逻辑迁移至后端）
DROP TRIGGER IF EXISTS `trg_auto_generate_purchase_number`;
