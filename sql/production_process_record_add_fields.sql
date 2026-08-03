-- 为 production_process_record 表添加关键工艺参数字段
-- 创建时间：2026-08-03

ALTER TABLE `production_process_record`
ADD COLUMN `process_params` TEXT NULL DEFAULT NULL COMMENT '关键工艺参数' AFTER `equipment_params`;
