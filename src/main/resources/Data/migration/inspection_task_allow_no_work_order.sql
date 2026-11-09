-- ===================================
-- 生产原料请检单不关联工单：允许检验任务不带工单
-- 生产原料请检单由请检页面创建，本身没有工单信息，自动生成的任务工单字段为空
-- 适用：已执行过 create_quality_pages_tables.sql / inspection_task_request_unique.sql 的数据库
-- ===================================

ALTER TABLE `inspection_task`
  MODIFY COLUMN `work_order_id` BIGINT NULL COMMENT '工单ID，生产原料请检单生成的 task 为空',
  MODIFY COLUMN `work_order_code` VARCHAR(50) NULL COMMENT '工单编号，生产原料请检单生成的 task 为空';
