-- ============================================================
-- 物料领料单模块建表脚本
-- 制剂室向医院药剂科申请领料
-- ============================================================

-- 1. 领料单主表
CREATE TABLE IF NOT EXISTS material_requisition_slip (
    id                    BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    slip_code             VARCHAR(50)  NOT NULL COMMENT '领料单号(LL-YYYYMMDD-NNN)',
    production_plan_id    BIGINT       NULL     COMMENT '关联生产计划ID',
    production_plan_code  VARCHAR(50)  NULL     COMMENT '关联生产计划编号',
    production_plan_title VARCHAR(200) NULL     COMMENT '生产计划标题',
    from_dept             VARCHAR(100) NOT NULL COMMENT '领用科室(固定:制剂室)',
    to_dept               VARCHAR(100) NOT NULL COMMENT '发放科室(固定:医院药剂科)',
    applicant             VARCHAR(50)  NOT NULL COMMENT '领料人',
    apply_time            DATETIME     NOT NULL COMMENT '领料时间',
    status                VARCHAR(20)  NOT NULL DEFAULT '待发放' COMMENT '状态(待发放/验收中/已入库/已作废)',
    acceptance_order_id   BIGINT       NULL     COMMENT '关联验收单ID',
    remark                VARCHAR(500) NULL     COMMENT '备注',
    is_deleted            TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记',
    version               INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    created_by            BIGINT       NULL     COMMENT '创建人ID',
    updated_by            BIGINT       NULL     COMMENT '更新人ID',
    created_time          DATETIME     NULL     COMMENT '创建时间',
    updated_time          DATETIME     NULL     COMMENT '更新时间',
    UNIQUE KEY uk_slip_code (slip_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料领料单主表';

-- 2. 领料单明细表
CREATE TABLE IF NOT EXISTS material_requisition_slip_detail (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    slip_id         BIGINT        NOT NULL COMMENT '领料单ID',
    seq             INT           NOT NULL COMMENT '序号',
    material_id     BIGINT        NULL     COMMENT '物料ID',
    material_code   VARCHAR(50)   NOT NULL COMMENT '物料编码',
    material_name   VARCHAR(200)  NOT NULL COMMENT '物料名称',
    unit_name       VARCHAR(20)   NOT NULL COMMENT '单位(kg/g/L/袋/盒/瓶)',
    apply_qty       DECIMAL(12,2) NOT NULL COMMENT '请领数量',
    actual_qty      DECIMAL(12,2) NULL     COMMENT '实发数量(药剂科填写)',
    batch_number    VARCHAR(50)   NULL     COMMENT '入库批号',
    unit_price      DECIMAL(10,2) NULL     COMMENT '单价(保留2位小数)',
    is_deleted      TINYINT       NOT NULL DEFAULT 0 COMMENT '软删除标记',
    version         INT           NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    INDEX idx_slip_id (slip_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料领料单明细表';

-- 3. 货物验收单扩展字段：关联领料单ID
ALTER TABLE acceptance_order ADD COLUMN related_slip_id BIGINT NULL COMMENT '关联领料单ID' AFTER related_order;
