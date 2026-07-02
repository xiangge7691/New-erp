-- ============================================================
-- 修复车间记录表 inspection_area 列允许为空
-- 涉及表：clean_inspection_record, temperature_humidity_record, pressure_difference_record
-- ============================================================

ALTER TABLE `clean_inspection_record` 
  MODIFY COLUMN `inspection_area` varchar(100) DEFAULT NULL COMMENT '检测区域',
  MODIFY COLUMN `inspector` varchar(50) DEFAULT NULL COMMENT '检测人';

ALTER TABLE `temperature_humidity_record` 
  MODIFY COLUMN `inspection_area` varchar(100) DEFAULT NULL COMMENT '检测区域';

ALTER TABLE `pressure_difference_record` 
  MODIFY COLUMN `inspection_area` varchar(100) DEFAULT NULL COMMENT '检测区域';
