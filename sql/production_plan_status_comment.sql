-- ===================================
-- 生产计划表 current_status 字段注释补充
-- 说明：补充 current_status 列的状态枚举注释，方便数据库端查看状态含义
-- ===================================

ALTER TABLE `production_plan`
MODIFY COLUMN `current_status` VARCHAR(20) DEFAULT NULL COMMENT '当前状态（待生产-计划未关联工单/生产中-存在未出库工单/已完成-所有工单已出库或已归档）';
