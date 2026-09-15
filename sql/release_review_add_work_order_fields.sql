-- 审核放行表添加工单关联字段
-- 将 work_order_id、work_order_code、preparation_name 从非持久化字段改为数据库列
-- 用于前端传入并持久化工单关联信息，不再完全依赖检验记录间接填充

ALTER TABLE `release_review`
ADD COLUMN `work_order_id` BIGINT DEFAULT NULL COMMENT '关联生产任务ID' AFTER `related_inspection_record_code`;

ALTER TABLE `release_review`
ADD COLUMN `work_order_code` VARCHAR(50) DEFAULT NULL COMMENT '关联生产任务编号' AFTER `work_order_id`;

ALTER TABLE `release_review`
ADD COLUMN `preparation_name` VARCHAR(100) DEFAULT NULL COMMENT '关联制剂名称' AFTER `object_name`;

-- 添加索引
ALTER TABLE `release_review`
ADD KEY `idx_work_order_id` (`work_order_id`);