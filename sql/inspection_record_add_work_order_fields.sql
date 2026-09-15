-- 检验记录表添加工单关联字段
-- 将 work_order_id、work_order_code、preparation_name 从非持久化字段改为数据库列
-- 用于前端传入并持久化工单关联信息，不再完全依赖请检记录间接填充

ALTER TABLE `inspection_record`
ADD COLUMN `work_order_id` BIGINT DEFAULT NULL COMMENT '关联生产任务ID' AFTER `related_inspection_request_code`;

ALTER TABLE `inspection_record`
ADD COLUMN `work_order_code` VARCHAR(50) DEFAULT NULL COMMENT '关联生产任务编号' AFTER `work_order_id`;

ALTER TABLE `inspection_record`
ADD COLUMN `preparation_name` VARCHAR(100) DEFAULT NULL COMMENT '关联制剂名称' AFTER `item_category`;

-- 添加索引
ALTER TABLE `inspection_record`
ADD KEY `idx_work_order_id` (`work_order_id`);
