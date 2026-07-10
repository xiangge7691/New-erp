-- ===================================
-- 文件管理：回收站 + 操作日志
-- ===================================

-- file_info 表新增回收站字段
ALTER TABLE file_info ADD COLUMN deleted_at DATETIME NULL COMMENT '删除时间（用于回收站）' AFTER is_deleted;
ALTER TABLE file_info ADD COLUMN deleted_by BIGINT NULL COMMENT '删除人ID' AFTER deleted_at;
ALTER TABLE file_info ADD COLUMN original_path VARCHAR(500) NULL COMMENT '原始路径（回收站恢复用）' AFTER deleted_by;

-- 新建文件操作日志表
CREATE TABLE file_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NULL COMMENT '关联file_info表',
    file_name VARCHAR(255) NOT NULL COMMENT '文件/文件夹名称',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    operation_type VARCHAR(30) NOT NULL COMMENT '操作类型：UPLOAD/DOWNLOAD/PREVIEW/CREATE_FOLDER/DELETE/RESTORE/RENAME/MOVE/COPY',
    root_type VARCHAR(20) NOT NULL COMMENT '根目录类型：business/custom',
    user_id BIGINT NOT NULL COMMENT '操作人ID',
    user_name VARCHAR(50) NULL COMMENT '操作人姓名',
    detail VARCHAR(500) NULL COMMENT '操作详情',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_file_id (file_id),
    INDEX idx_user_id (user_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件操作日志表';
