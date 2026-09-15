-- ===================================
-- work_order 表增加 audit_release_time 和 inbound_time 字段
-- 用于记录工单的审核放行时间和入库时间
-- ===================================

-- 1. 查看当前表结构
DESCRIBE work_order;

-- 2. 添加 audit_release_time 字段（审核放行时间）
ALTER TABLE work_order
    ADD COLUMN audit_release_time DATETIME DEFAULT NULL COMMENT '审核放行时间' AFTER archive_time;

-- 3. 添加 inbound_time 字段（入库时间）
ALTER TABLE work_order
    ADD COLUMN inbound_time DATETIME DEFAULT NULL COMMENT '入库时间' AFTER audit_release_time;

-- 4. 验证结果
DESCRIBE work_order;
