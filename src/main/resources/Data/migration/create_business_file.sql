-- ===================================
-- 业务附件关系表
-- 说明：
--   1. file_info 继续保存物理文件元数据；
--   2. business_file 保存“业务记录 -> 文件”的绑定关系；
--   3. 关系行解绑时物理删除，is_deleted 固定为 0，供 MyBatis-Plus 查询过滤。
-- ===================================
CREATE TABLE IF NOT EXISTS business_file (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    biz_type      VARCHAR(64)  NOT NULL COMMENT '业务类型编码，一表一码',
    biz_id        BIGINT       NOT NULL COMMENT '业务表主键值',
    ref_id        BIGINT       NOT NULL DEFAULT 0 COMMENT '子对象ID（如审批节点ID），无子对象时为0',
    file_id       BIGINT       NOT NULL COMMENT '文件ID（file_info.file_id）',
    file_kind     VARCHAR(64)  NOT NULL DEFAULT 'DEFAULT' COMMENT '文件槽位/用途',
    sort_order    INT          NOT NULL DEFAULT 0 COMMENT '排序',
    is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除（关系行实际物理删除，保留字段用于统一查询）',
    created_by    BIGINT       NULL COMMENT '创建人',
    created_time  DATETIME     NULL COMMENT '创建时间',
    updated_by    BIGINT       NULL COMMENT '更新人',
    updated_time  DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_file (biz_type, biz_id, ref_id, file_id),
    KEY idx_biz (biz_type, biz_id, ref_id, file_kind),
    KEY idx_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务附件关联表';
