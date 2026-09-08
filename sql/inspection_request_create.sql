-- ============================================================
-- 请检记录模块建表脚本
-- 生产过程向检验部门提出检验申请的记录
-- ============================================================

CREATE TABLE IF NOT EXISTS inspection_request (
    id                    BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    inspection_code       VARCHAR(50)   NOT NULL COMMENT '请检编号(QJ-YYYYMMDD-NNN)',
    work_order_id         BIGINT        NULL     COMMENT '关联生产任务ID',
    work_order_code       VARCHAR(50)   NULL     COMMENT '关联生产任务编号',
    inspection_plan_code  VARCHAR(50)   NULL     COMMENT '关联检验计划编号(选填)',
    preparation_code      VARCHAR(50)   NULL     COMMENT '制剂编码',
    preparation_name      VARCHAR(200)  NULL     COMMENT '制剂名称',
    inspection_item_name  VARCHAR(200)  NOT NULL COMMENT '被检物名称(默认取制剂名称)',
    batch_number          VARCHAR(50)   NULL     COMMENT '批号',
    spec                  VARCHAR(200)  NULL     COMMENT '规格',
    process_name          VARCHAR(200)  NOT NULL COMMENT '工序(手动输入)',
    config_quantity       VARCHAR(100)  NOT NULL COMMENT '配置量(文本，如2000或3000kg)',
    request_department    VARCHAR(200)  NOT NULL COMMENT '请检部门(下拉选择)',
    requester             VARCHAR(50)   NOT NULL COMMENT '请检人',
    request_time          DATETIME      NOT NULL COMMENT '请检时间',
    inspection_items      TEXT          NOT NULL COMMENT '请检项目(多项，顿号分隔)',
    remark                VARCHAR(500)  NULL     COMMENT '备注',
    is_deleted            TINYINT       NOT NULL DEFAULT 0 COMMENT '软删除标记',
    version               INT           NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    created_by            BIGINT        NULL     COMMENT '创建人ID',
    updated_by            BIGINT        NULL     COMMENT '更新人ID',
    created_time          DATETIME      NULL     COMMENT '创建时间',
    updated_time          DATETIME      NULL     COMMENT '更新时间',
    UNIQUE KEY uk_inspection_code (inspection_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请检记录表';
