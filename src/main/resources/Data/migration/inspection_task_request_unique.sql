-- ===================================
-- 请检单自动生成检验任务：任务唯一约束调整
-- 一张请检单对应一张检验任务；同一工单+品类+批号下允许多条任务
-- （同一批次的中间产品会在不同工序分别请检，各自生成一条任务）
-- 适用：已执行过 create_quality_pages_tables.sql 的数据库
-- ===================================

-- 1. 原“工单+品类+批号”唯一约束不再适用，改为普通索引
ALTER TABLE `inspection_task` DROP INDEX `uk_inspection_task_batch`;
ALTER TABLE `inspection_task`
  ADD KEY `idx_inspection_task_batch` (`work_order_id`, `item_category`, `batch_no`);

-- 2. 一张请检单只能生成一张任务（request_ref_id 为 NULL 的任务不受限制）
ALTER TABLE `inspection_task`
  ADD UNIQUE KEY `uk_inspection_task_request` (`request_ref_id`);
