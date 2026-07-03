-- ============================================================
-- 为工序表添加「设备关键参数」字段
-- ============================================================

ALTER TABLE `preparation_process_template` 
  ADD COLUMN `equipment_params` VARCHAR(500) DEFAULT NULL COMMENT '设备关键参数' AFTER `equipment_desc`;

ALTER TABLE `production_process_record` 
  ADD COLUMN `equipment_params` VARCHAR(500) DEFAULT NULL COMMENT '设备关键参数' AFTER `equipment`;
