-- ===================================
-- 为 purchase_order_items 表添加乐观锁版本号字段
-- ===================================

-- 添加 version 字段
ALTER TABLE `purchase_order_items`
ADD COLUMN `version` INT DEFAULT 1 COMMENT '乐观锁版本号' AFTER `is_deleted`;
