-- ===================================
-- 业务附件迁移冲突清单
-- 说明：迁移脚本无法自动判定归属的旧文件写入本表，人工确认后再补迁移。
-- ===================================
CREATE TABLE IF NOT EXISTS business_file_migration_conflict (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '冲突ID',
    file_id       BIGINT       NOT NULL COMMENT '原文件ID',
    business_type VARCHAR(64)  NULL COMMENT '原业务类型',
    business_id   BIGINT       NULL COMMENT '原业务ID',
    custom_path   VARCHAR(512) NULL COMMENT '原自定义路径',
    reason        VARCHAR(255) NULL COMMENT '冲突原因',
    created_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件迁移冲突清单';
