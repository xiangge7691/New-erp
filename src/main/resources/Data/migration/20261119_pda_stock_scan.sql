-- ===================================
-- PDA 仓储扫码：出库/退库明细扫码状态
-- 测试库：192.168.5.100 / erp_db
-- ===================================

ALTER TABLE `stock_out_detail`
  ADD COLUMN `scan_status` VARCHAR(20) NOT NULL DEFAULT '待扫码' COMMENT '扫码状态：待扫码/已扫',
  ADD COLUMN `scanned_by` BIGINT NULL COMMENT '扫码人ID',
  ADD COLUMN `scanned_time` DATETIME NULL COMMENT '扫码时间';

ALTER TABLE `return_order_detail`
  ADD COLUMN `scan_status` VARCHAR(20) NOT NULL DEFAULT '待扫码' COMMENT '扫码状态：待扫码/已扫',
  ADD COLUMN `scanned_by` BIGINT NULL COMMENT '扫码人ID',
  ADD COLUMN `scanned_time` DATETIME NULL COMMENT '扫码时间';
