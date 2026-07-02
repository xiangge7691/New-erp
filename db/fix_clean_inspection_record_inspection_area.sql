-- ============================================================
-- 修复 clean_inspection_record 表 NOT NULL 且无默认值的列
-- ============================================================

ALTER TABLE `clean_inspection_record` 
  MODIFY COLUMN `inspection_area` varchar(100) DEFAULT NULL COMMENT '检测区域',
  MODIFY COLUMN `inspector` varchar(50) DEFAULT NULL COMMENT '检测人';
