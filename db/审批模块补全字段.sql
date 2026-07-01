-- 审批模块补全字段
-- 注意：使用 AFTER 语法需要引用已存在的列，如果列不存在则不指定位置

-- approval_instance 新增字段
ALTER TABLE approval_instance
    ADD COLUMN `initiator_id` BIGINT DEFAULT NULL COMMENT '发起人ID',
    ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除';

-- approval_node 新增字段
ALTER TABLE approval_node
    ADD COLUMN `reject_to_node_id` BIGINT DEFAULT NULL COMMENT '驳回到哪个节点（为null时驳回到第一个节点）';

-- approval_workflow 新增字段
ALTER TABLE approval_workflow
    ADD COLUMN `status` INT NOT NULL DEFAULT 1 COMMENT '状态：0停用/1启用',
    ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除';

-- approval_record 新增字段
ALTER TABLE approval_record
    ADD COLUMN `approved_at` DATETIME DEFAULT NULL COMMENT '审批时间',
    ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除';
