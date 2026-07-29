-- ===================================
-- 房间信息表新增字段
-- ===================================

-- 1. 新增功能间分类字段
ALTER TABLE `room_info`
ADD COLUMN `function_type` VARCHAR(50) DEFAULT '' COMMENT '功能间分类（车间/仓储/检验/工程）' AFTER `cleaning_cycle`;

-- 2. 新增涉及工序字段
ALTER TABLE `room_info`
ADD COLUMN `related_processes` VARCHAR(500) DEFAULT '' COMMENT '涉及工序（多个以逗号分隔）' AFTER `function_type`;
