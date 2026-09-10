-- 检验记录表添加关联请检编号字段
-- 用于关联请检记录，从而获取生产任务编号和制剂信息

ALTER TABLE `inspection_record`
ADD COLUMN `related_inspection_request_code` VARCHAR(50) DEFAULT NULL COMMENT '关联请检编号（可空，用于关联请检记录获取工单和制剂信息）' AFTER `related_sampling_code`;

-- 添加索引
ALTER TABLE `inspection_record`
ADD KEY `idx_related_inspection_request_code` (`related_inspection_request_code`);
