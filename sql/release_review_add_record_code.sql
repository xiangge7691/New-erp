-- 审核放行表添加关联检验记录编号字段
-- 用于关联检验记录，从而获取生产任务编号和制剂信息

ALTER TABLE `release_review`
ADD COLUMN `related_inspection_record_code` VARCHAR(50) DEFAULT NULL COMMENT '关联检验记录编号（可空，用于关联检验记录获取工单和制剂信息）' AFTER `related_inspection_code`;

-- 添加索引
ALTER TABLE `release_review`
ADD KEY `idx_related_inspection_record_code` (`related_inspection_record_code`);
