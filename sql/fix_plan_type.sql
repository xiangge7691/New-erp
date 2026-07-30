-- 修改生产计划类型字段为字符串类型
ALTER TABLE `production_plan`
MODIFY COLUMN `plan_type` VARCHAR(50) DEFAULT '' COMMENT '生产计划类型（自主加工/委托加工/试生产）';
