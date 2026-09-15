-- ===================================
-- file_operation_log 表增加 business_type 字段
-- 用于记录文件操作日志的业务类型
-- ===================================

-- 1. 查看当前表结构
DESCRIBE file_operation_log;

-- 2. 添加 business_type 字段（可为空，兼容已有数据）
ALTER TABLE file_operation_log
    ADD COLUMN business_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型：货物验收随货清单(GOODS_ACCEPTANCE_WAYBILL)、货物验收发票(GOODS_ACCEPTANCE_INVOICE)、货物验收检验报告(GOODS_ACCEPTANCE_INSPECTION_REPORT)、审核放行记录(AUDIT_RELEASE_RECORD)、留样记录(SAMPLE_RETENTION_RECORD)、通用文件(GENERAL)' AFTER root_type;

-- 3. 添加索引（可选，提升按业务类型筛选的查询性能）
ALTER TABLE file_operation_log
    ADD INDEX idx_business_type (business_type);

-- 4. 验证结果
DESCRIBE file_operation_log;
