-- ===================================
-- 货物验收模块建表 + 库存列结构兼容脚本（简洁版）
--
-- 说明：
--   1. 本脚本为"直白版"：所有列改造直接使用 ALTER TABLE ... MODIFY COLUMN，
--      不使用 information_schema 动态判断（旧脚本 sql/acceptance_create_tables.sql
--      已执行过，此处为简化后的最终形态）
--   2. MODIFY COLUMN 为幂等操作，本脚本可重复执行
--   3. 状态/类型列统一改为 VARCHAR(30)，用于存储中文值：
--      - 入库类型：采购入库/成品入库/直接入库
--      - 出库类型：生产领料出库/销售出库/报损出库
--      - 单据状态：草稿/已入库/已出库/已取消
--      - 库存状态：合格/待检/不合格
--      - 流水类型：入库类型/出库类型中文值
--   4. 注意：stock_in_detail.stock_status 列已由旧脚本添加，
--      本脚本不重复添加（新环境如需该列请先执行旧脚本）
-- 执行方式：在 erp_db 数据库直接执行
-- ===================================

-- ===================================
-- 一、验收模块建表（幂等）
-- ===================================

-- 1. 验收单主表
CREATE TABLE IF NOT EXISTS `acceptance_order` (
  `acceptance_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '验收单唯一标识',
  `acceptance_code`       VARCHAR(50)  NOT NULL COMMENT '验收单号（唯一，格式YS-YYYYMMDD-NNN）',
  `source_type`           VARCHAR(20)  DEFAULT '采购入库' COMMENT '来源类型：采购入库/成品入库/直接入库',
  `related_order`         VARCHAR(50)  DEFAULT NULL COMMENT '关联采购订单号',
  `purchase_number`       VARCHAR(50)  DEFAULT NULL COMMENT '关联采购计划编号',
  `plan_code`             VARCHAR(50)  DEFAULT NULL COMMENT '关联生产计划编号',
  `work_order_code`       VARCHAR(50)  DEFAULT NULL COMMENT '关联生产任务编号',
  `title`                 VARCHAR(200) DEFAULT NULL COMMENT '生产计划标题',
  `unit_name`             VARCHAR(100) DEFAULT NULL COMMENT '收货单位名称',
  `preparation_code`      VARCHAR(50)  DEFAULT NULL COMMENT '关联制剂编码',
  `preparation_name`      VARCHAR(100) DEFAULT NULL COMMENT '关联制剂名称',
  `spec`                  VARCHAR(100) DEFAULT NULL COMMENT '制剂规格',
  `batch_qty`             DECIMAL(18,3) DEFAULT NULL COMMENT '计划生产批量',
  `prescription_multiple` DECIMAL(18,3) DEFAULT NULL COMMENT '处方生产倍数',
  `prod_unit_id`          BIGINT       DEFAULT NULL COMMENT '入库仓库（生产单位ID，检验合格时选择）',
  `status`                VARCHAR(20)  NOT NULL DEFAULT '到货初验' COMMENT '状态：运输中/到货初验/物料检验/已入库/待退货/已退换',
  `delivery_date`         DATE         DEFAULT NULL COMMENT '预计交付日期（交期）',
  `remark`                VARCHAR(500) DEFAULT NULL COMMENT '备注（流程节点自动追加）',
  `original_acceptance_code` VARCHAR(50) DEFAULT NULL COMMENT '原验收单号（重新收货时记录）',
  `approval_instance_id`  BIGINT       DEFAULT NULL COMMENT '审批实例ID',
  `is_deleted`            TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`               INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by`            BIGINT       DEFAULT NULL COMMENT '创建人ID',
  `updated_by`            BIGINT       DEFAULT NULL COMMENT '更新人ID',
  `created_time`          DATETIME     DEFAULT NULL COMMENT '创建时间',
  `updated_time`          DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`acceptance_id`),
  UNIQUE KEY `uk_acceptance_code` (`acceptance_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货物验收单主表';

-- 2. 验收明细表
CREATE TABLE IF NOT EXISTS `acceptance_detail` (
  `detail_id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '验收明细唯一标识',
  `acceptance_id`     BIGINT        NOT NULL COMMENT '关联验收单ID',
  `seq`               INT           NOT NULL DEFAULT 0 COMMENT '明细序号',
  `item_type`         VARCHAR(20)   DEFAULT 'material' COMMENT '物品类型：material物料/preparation制剂',
  `item_id`           BIGINT        DEFAULT NULL COMMENT '物品ID（引用物料或制剂表）',
  `material_code`     VARCHAR(50)   DEFAULT NULL COMMENT '物料编码',
  `material_name`     VARCHAR(100)  NOT NULL COMMENT '物料名称',
  `material_category` VARCHAR(20)   DEFAULT NULL COMMENT '物料分类：原料/辅料/包材/成品',
  `unit_name`         VARCHAR(20)   DEFAULT NULL COMMENT '计量单位：kg/g/L/袋/盒/瓶',
  `standard_dosage`   DECIMAL(18,4) DEFAULT NULL COMMENT '标准处方量',
  `quantity`          DECIMAL(18,3) NOT NULL COMMENT '采购数量',
  `actual_arrival_qty` DECIMAL(18,3) DEFAULT NULL COMMENT '实际到货数量（供应商实际送达数量，金额以该数量为准计算）',
  `inbound_qty`       DECIMAL(18,3) DEFAULT NULL COMMENT '入库数量（检验合格后实际入库数量）',
  `unit_price`        DECIMAL(18,2) DEFAULT NULL COMMENT '物料单价',
  `amount`            DECIMAL(18,2) DEFAULT NULL COMMENT '金额（实际到货数量*单价）',
  `batch_number`      VARCHAR(50)   DEFAULT NULL COMMENT '物料批号（入库必填）',
  `expiry_date`       DATE          DEFAULT NULL COMMENT '有效期至',
  `diff_quantity`     DECIMAL(18,4) DEFAULT NULL COMMENT '标准量差值（实际到货数量与标准处方量的差值）',
  `is_deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`           INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by`        BIGINT        DEFAULT NULL COMMENT '创建人ID',
  `updated_by`        BIGINT        DEFAULT NULL COMMENT '更新人ID',
  `created_time`      DATETIME      DEFAULT NULL COMMENT '创建时间',
  `updated_time`      DATETIME      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_acceptance_id` (`acceptance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货物验收单明细表';

-- ===================================
-- 二、库存相关列结构兼容（直接 MODIFY，幂等）
-- ===================================

-- 3. 状态/类型列统一改为 VARCHAR，支持中文值
ALTER TABLE `stock` MODIFY COLUMN `stock_status` VARCHAR(20) DEFAULT '合格' COMMENT '库存状态：合格/待检/不合格';
ALTER TABLE `stock_transaction` MODIFY COLUMN `transaction_type` VARCHAR(30) DEFAULT NULL COMMENT '交易类型：入库类型/出库类型中文值';
ALTER TABLE `stock_in` MODIFY COLUMN `in_type` VARCHAR(30) DEFAULT NULL COMMENT '入库类型：采购入库/成品入库/直接入库';
ALTER TABLE `stock_in` MODIFY COLUMN `in_status` VARCHAR(30) DEFAULT '草稿' COMMENT '入库单状态：草稿/已入库/已取消';
ALTER TABLE `stock_out` MODIFY COLUMN `out_type` VARCHAR(30) DEFAULT NULL COMMENT '出库类型：生产领料出库/销售出库/报损出库';
ALTER TABLE `stock_out` MODIFY COLUMN `out_status` VARCHAR(30) DEFAULT '草稿' COMMENT '出库单状态：草稿/已出库/已取消';

-- 4. stock 表业务列允许为空（库存以 item_code+仓库+批号为唯一键，item_id 等冗余字段可为空）
ALTER TABLE `stock` MODIFY COLUMN `item_id` BIGINT DEFAULT NULL COMMENT '关联的物品ID（可空）';
ALTER TABLE `stock` MODIFY COLUMN `unit_price` DECIMAL(18,2) DEFAULT NULL COMMENT '单价';
ALTER TABLE `stock` MODIFY COLUMN `min_quantity` DECIMAL(18,3) DEFAULT NULL COMMENT '最低库存预警数量';
ALTER TABLE `stock` MODIFY COLUMN `max_quantity` DECIMAL(18,3) DEFAULT NULL COMMENT '最高库存限制数量';
ALTER TABLE `stock` MODIFY COLUMN `production_date` DATE DEFAULT NULL COMMENT '生产日期';
ALTER TABLE `stock` MODIFY COLUMN `expiry_date` DATE DEFAULT NULL COMMENT '有效期至';
ALTER TABLE `stock` MODIFY COLUMN `storage_location` VARCHAR(100) DEFAULT NULL COMMENT '库位/货架号';
ALTER TABLE `stock` MODIFY COLUMN `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注';

-- 5. 入库明细业务列允许为空
ALTER TABLE `stock_in_detail` MODIFY COLUMN `item_id` BIGINT DEFAULT NULL COMMENT '物品ID（可空）';
ALTER TABLE `stock_in_detail` MODIFY COLUMN `production_date` DATE DEFAULT NULL COMMENT '生产日期';
ALTER TABLE `stock_in_detail` MODIFY COLUMN `unit_price` DECIMAL(18,2) DEFAULT NULL COMMENT '单价';
ALTER TABLE `stock_in_detail` MODIFY COLUMN `amount` DECIMAL(18,2) DEFAULT NULL COMMENT '金额';
ALTER TABLE `stock_in_detail` MODIFY COLUMN `storage_location` VARCHAR(100) DEFAULT NULL COMMENT '存放位置';
ALTER TABLE `stock_in_detail` MODIFY COLUMN `expiry_date` DATE DEFAULT NULL COMMENT '有效期至';
ALTER TABLE `stock_in_detail` MODIFY COLUMN `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注';

-- 6. 出库明细业务列允许为空
ALTER TABLE `stock_out_detail` MODIFY COLUMN `item_id` BIGINT DEFAULT NULL COMMENT '物品ID（可空）';
ALTER TABLE `stock_out_detail` MODIFY COLUMN `unit_price` DECIMAL(18,2) DEFAULT NULL COMMENT '单价';
ALTER TABLE `stock_out_detail` MODIFY COLUMN `amount` DECIMAL(18,2) DEFAULT NULL COMMENT '金额';
ALTER TABLE `stock_out_detail` MODIFY COLUMN `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注';

-- ===================================
-- 三、旧英文枚举数据迁移为中文（幂等，仅当存在旧值时更新）
-- ===================================

-- 7. 库存状态：normal → 合格
UPDATE `stock` SET `stock_status` = '合格' WHERE `stock_status` = 'normal';

-- 8. 库存流水类型：in/out/adjust → 中文
UPDATE `stock_transaction` SET `transaction_type` = '入库' WHERE `transaction_type` = 'in';
UPDATE `stock_transaction` SET `transaction_type` = '出库' WHERE `transaction_type` = 'out';
UPDATE `stock_transaction` SET `transaction_type` = '调整' WHERE `transaction_type` = 'adjust';

-- 9. 入库单状态：旧英文 → 中文
UPDATE `stock_in` SET `in_status` = '草稿' WHERE `in_status` IN ('draft', 'submitted');
UPDATE `stock_in` SET `in_status` = '已入库' WHERE `in_status` = 'completed';
UPDATE `stock_in` SET `in_status` = '已取消' WHERE `in_status` = 'cancelled';

-- 10. 出库单状态：旧英文 → 中文
UPDATE `stock_out` SET `out_status` = '草稿' WHERE `out_status` IN ('draft', 'submitted');
UPDATE `stock_out` SET `out_status` = '已出库' WHERE `out_status` = 'completed';
UPDATE `stock_out` SET `out_status` = '已取消' WHERE `out_status` = 'cancelled';
