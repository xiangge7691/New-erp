-- ===================================
-- 设备状态字段修改
-- ===================================

-- 1. 先更新现有数据（必须在修改字段类型之前执行）
UPDATE `equipment` SET `equipment_status` = '正常' WHERE `equipment_status` = '1';
UPDATE `equipment` SET `equipment_status` = '停用' WHERE `equipment_status` = '0';

-- 2. 修改设备状态字段类型
ALTER TABLE `equipment`
MODIFY COLUMN `equipment_status` VARCHAR(20) DEFAULT '正常' COMMENT '设备状态（正常/停用/报废）';
