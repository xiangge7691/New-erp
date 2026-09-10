-- 迁移脚本：出库日期、入库日期精确到时分秒
-- 将 stock_out.out_date、stock_in.in_date 从 date 升级为 datetime
-- 需手动执行：mysql -h 8.134.48.54 -P 3306 -u erp_db -p erp_db < stock_in_out_date_datetime.sql

-- 出库单：出库日期精确到时分秒
ALTER TABLE `stock_out` MODIFY COLUMN `out_date` DATETIME DEFAULT NULL COMMENT '出库日期';

-- 入库单：入库日期精确到时分秒
ALTER TABLE `stock_in` MODIFY COLUMN `in_date` DATETIME DEFAULT NULL COMMENT '入库日期';
