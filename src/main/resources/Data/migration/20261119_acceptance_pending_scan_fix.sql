-- ===================================
-- 修正验收单提前标记“已入库”的数据
-- 背景：验收合格后先生成待打印/待扫码的草稿入库单，库存尚未增加，
--       但旧逻辑同时把验收单、验收明细、采购订单标记为已入库。
-- 处理：存在草稿入库单（待打印/待扫码）的验收单回退为“待扫码入库”，
--       采购订单回退为“验收中”，采购订单明细回退为“待入库”，
--       待 PDA 打印标签、逐条扫码并签名确认后再由业务代码更新为已入库。
-- 说明：幂等，可重复执行。
-- ===================================

-- 1. 验收单主单：已入库 → 待扫码入库
UPDATE `acceptance_order` a
JOIN `stock_in` s ON s.`related_order` = a.`acceptance_code` AND s.`is_deleted` = 0
SET a.`status` = '待扫码入库', a.`updated_time` = NOW()
WHERE a.`is_deleted` = 0
  AND a.`status` = '已入库'
  AND s.`in_status` = '草稿'
  AND s.`scan_status` IN ('待打印', '待扫码');

-- 2. 验收明细：已入库 → 待扫码入库
UPDATE `acceptance_detail` d
JOIN `acceptance_order` a ON a.`acceptance_id` = d.`acceptance_id` AND a.`is_deleted` = 0
JOIN `stock_in` s ON s.`related_order` = a.`acceptance_code` AND s.`is_deleted` = 0
SET d.`status` = '待扫码入库', d.`updated_time` = NOW()
WHERE d.`is_deleted` = 0
  AND d.`status` = '已入库'
  AND a.`status` = '待扫码入库'
  AND s.`in_status` = '草稿'
  AND s.`scan_status` IN ('待打印', '待扫码');

-- 3. 采购订单主单：已入库 → 验收中（库存尚未实际入账）
UPDATE `purchase_orders` p
JOIN `acceptance_order` a ON a.`related_order` = p.`purchase_number`
SET p.`status` = '验收中', p.`updated_time` = NOW()
WHERE a.`status` = '待扫码入库'
  AND p.`status` = '已入库'
  AND p.`is_deleted` = 0;

-- 4. 采购订单明细：已入库 → 待入库
UPDATE `purchase_order_items` i
JOIN `acceptance_detail` d ON d.`purchase_item_id` = i.`id`
JOIN `acceptance_order` a ON a.`acceptance_id` = d.`acceptance_id`
SET i.`status` = '待入库'
WHERE d.`status` = '待扫码入库'
  AND a.`status` = '待扫码入库'
  AND i.`status` = '已入库'
  AND i.`is_deleted` = 0;
