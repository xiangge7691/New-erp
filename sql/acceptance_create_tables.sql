-- ===================================
-- 货物验收模块建表脚本
-- 表：acceptance_order（验收单主表）
--     acceptance_detail（验收明细表）
-- 补充：stock_in_detail 增加库存状态列（入库时携带"合格/待检"）
-- 说明：验收单使用独立表，通过 related_order / purchase_number / plan_code 关联采购订单与生产计划
-- 状态流转：运输中 → 到货初验 → 物料检验 → 已入库 / 待退货 → 已退换
-- 执行方式：在 erp_db 数据库直接执行本脚本（可重复执行，使用 IF NOT EXISTS）
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
  `quantity`          DECIMAL(18,3) NOT NULL COMMENT '采购数量（实际到货数量）',
  `unit_price`        DECIMAL(18,2) DEFAULT NULL COMMENT '物料单价',
  `amount`            DECIMAL(18,2) DEFAULT NULL COMMENT '金额（数量*单价）',
  `batch_number`      VARCHAR(50)   DEFAULT NULL COMMENT '物料批号（入库必填）',
  `expiry_date`       DATE          DEFAULT NULL COMMENT '有效期至',
  `diff_quantity`     DECIMAL(18,4) DEFAULT NULL COMMENT '标准量差值（采购数量与标准处方量的差值）',
  `is_deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '是否已删除：0否/1是',
  `version`           INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by`        BIGINT        DEFAULT NULL COMMENT '创建人ID',
  `updated_by`        BIGINT        DEFAULT NULL COMMENT '更新人ID',
  `created_time`      DATETIME      DEFAULT NULL COMMENT '创建时间',
  `updated_time`      DATETIME      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_acceptance_id` (`acceptance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货物验收单明细表';

-- 3. stock_in_detail 增加库存状态列（入库时携带 合格/待检，确认入库后写入 stock 表）
-- MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，使用 information_schema 判断后动态执行
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_in_detail' AND COLUMN_NAME = 'stock_status');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `stock_in_detail` ADD COLUMN `stock_status` VARCHAR(20) DEFAULT ''合格'' COMMENT ''库存状态：合格/待检/不合格''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. stock 表 stock_status 列扩展为 VARCHAR(20)（兼容"合格/待检/不合格"中文值，避免 Data truncated 报错）
SET @col_type := (SELECT DATA_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock' AND COLUMN_NAME = 'stock_status');
SET @ddl2 := IF(COALESCE(@col_type, '') <> 'varchar',
    'ALTER TABLE `stock` MODIFY COLUMN `stock_status` VARCHAR(20) DEFAULT ''合格'' COMMENT ''库存状态：合格/待检/不合格''',
    'SELECT 1');
PREPARE stmt2 FROM @ddl2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 5. 通用：将 stock 表中可能为空的业务列放宽为 NULL（保留原类型，幂等）
-- 验收/入库明细可能不携带 item_id/production_date/unit_price 等字段，避免 Data truncated 报错
-- 每个列：仅当当前为 NOT NULL 时才执行 MODIFY，否则跳过
SET @col := (SELECT CONCAT('ALTER TABLE `stock` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock' AND COLUMN_NAME = 'item_id' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT CONCAT('ALTER TABLE `stock` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock' AND COLUMN_NAME = 'unit_price' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT CONCAT('ALTER TABLE `stock` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock' AND COLUMN_NAME = 'min_quantity' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT CONCAT('ALTER TABLE `stock` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock' AND COLUMN_NAME = 'max_quantity' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT CONCAT('ALTER TABLE `stock` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock' AND COLUMN_NAME = 'production_date' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT CONCAT('ALTER TABLE `stock` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock' AND COLUMN_NAME = 'expiry_date' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT CONCAT('ALTER TABLE `stock` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock' AND COLUMN_NAME = 'storage_location' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT CONCAT('ALTER TABLE `stock` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock' AND COLUMN_NAME = 'remark' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

-- 6. stock_transaction 表 transaction_type 列扩展为 VARCHAR(30)
-- （流水类型存储入库/出库类型中文值如"采购入库/生产领料出库"，兼容 enum 或 varchar(10) 旧定义）
SET @len := (SELECT COALESCE(CHARACTER_MAXIMUM_LENGTH, 0) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_transaction' AND COLUMN_NAME = 'transaction_type');
SET @ddl := IF(@len < 30,
    'ALTER TABLE `stock_transaction` MODIFY COLUMN `transaction_type` VARCHAR(30) DEFAULT NULL COMMENT ''交易类型：入库类型/出库类型中文值''',
    'SELECT 1');
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

-- 7. stock_in.in_type 列扩展为 VARCHAR(30)（入库类型使用中文枚举"采购入库/成品入库/直接入库"）
SET @len := (SELECT COALESCE(CHARACTER_MAXIMUM_LENGTH, 0) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_in' AND COLUMN_NAME = 'in_type');
SET @ddl := IF(@len < 30,
    'ALTER TABLE `stock_in` MODIFY COLUMN `in_type` VARCHAR(30) DEFAULT NULL COMMENT ''入库类型：采购入库/成品入库/直接入库''',
    'SELECT 1');
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

-- 8. stock_out.out_type 列扩展为 VARCHAR(30)（出库类型使用中文枚举"生产领料出库/销售出库/报损出库"）
SET @len := (SELECT COALESCE(CHARACTER_MAXIMUM_LENGTH, 0) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_out' AND COLUMN_NAME = 'out_type');
SET @ddl := IF(@len < 30,
    'ALTER TABLE `stock_out` MODIFY COLUMN `out_type` VARCHAR(30) DEFAULT NULL COMMENT ''出库类型：生产领料出库/销售出库/报损出库''',
    'SELECT 1');
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

-- 9. stock_in.in_status 列扩展为 VARCHAR(30)（状态使用中文枚举"草稿/已入库/已取消"）
SET @len := (SELECT COALESCE(CHARACTER_MAXIMUM_LENGTH, 0) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_in' AND COLUMN_NAME = 'in_status');
SET @ddl := IF(@len < 30,
    'ALTER TABLE `stock_in` MODIFY COLUMN `in_status` VARCHAR(30) DEFAULT ''草稿'' COMMENT ''入库单状态：草稿/已入库/已取消''',
    'SELECT 1');
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

-- 10. stock_out.out_status 列扩展为 VARCHAR(30)（状态使用中文枚举"草稿/已出库/已取消"）
SET @len := (SELECT COALESCE(CHARACTER_MAXIMUM_LENGTH, 0) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_out' AND COLUMN_NAME = 'out_status');
SET @ddl := IF(@len < 30,
    'ALTER TABLE `stock_out` MODIFY COLUMN `out_status` VARCHAR(30) DEFAULT ''草稿'' COMMENT ''出库单状态：草稿/已出库/已取消''',
    'SELECT 1');
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

-- 11. stock_in_detail.item_id 允许为空（明细以 item_code 为主，item_id 仅作引用）
SET @col := (SELECT CONCAT('ALTER TABLE `stock_in_detail` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_in_detail' AND COLUMN_NAME = 'item_id' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

-- 12. stock_in_detail 其余可能为空的业务列放宽为 NULL（验收/前端明细可能不携带生产日期/库位等）
SET @col := (SELECT CONCAT('ALTER TABLE `stock_in_detail` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_in_detail' AND COLUMN_NAME = 'production_date' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT CONCAT('ALTER TABLE `stock_in_detail` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_in_detail' AND COLUMN_NAME = 'unit_price' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT CONCAT('ALTER TABLE `stock_in_detail` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_in_detail' AND COLUMN_NAME = 'storage_location' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT CONCAT('ALTER TABLE `stock_in_detail` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_in_detail' AND COLUMN_NAME = 'remark' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT CONCAT('ALTER TABLE `stock_in_detail` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_in_detail' AND COLUMN_NAME = 'amount' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;

SET @col := (SELECT CONCAT('ALTER TABLE `stock_in_detail` MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL')
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stock_in_detail' AND COLUMN_NAME = 'expiry_date' AND IS_NULLABLE = 'NO');
SET @ddl := IF(@col IS NULL, 'SELECT 1', @col);
PREPARE st FROM @ddl; EXECUTE st; DEALLOCATE PREPARE st;
