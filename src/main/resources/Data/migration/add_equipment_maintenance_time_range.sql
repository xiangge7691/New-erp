-- ===================================
-- 设备维保记录：维保时间由单一日期改为开始/结束时间区间
-- 1) 新增 maintenance_start_time / maintenance_end_time（DATETIME）
-- 2) 历史数据回填：开始 = maintenance_date 00:00:00，结束 = maintenance_date 23:59:59
-- 3) maintenance_date 放宽为可空（保留为历史字段，代码不再写入）
-- 4) 新增 (equipment_id, maintenance_start_time, maintenance_end_time) 组合索引，支撑与设备使用时间的冲突校验
-- 说明：除索引外均可重复执行；索引已存在时跳过对应语句
-- ===================================

-- 1. 新增维保开始时间
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'equipment_maintenance' AND COLUMN_NAME = 'maintenance_start_time');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `equipment_maintenance` ADD COLUMN `maintenance_start_time` DATETIME DEFAULT NULL COMMENT ''维保开始时间'' AFTER `maintenance_type`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 新增维保结束时间
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'equipment_maintenance' AND COLUMN_NAME = 'maintenance_end_time');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `equipment_maintenance` ADD COLUMN `maintenance_end_time` DATETIME DEFAULT NULL COMMENT ''维保结束时间'' AFTER `maintenance_start_time`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 历史数据回填：原维保日期按"当天整日维保"转换为时间区间（仅回填未转换过的记录）
UPDATE `equipment_maintenance`
   SET `maintenance_start_time` = TIMESTAMP(`maintenance_date`),
       `maintenance_end_time`   = TIMESTAMP(`maintenance_date`, '23:59:59')
 WHERE `maintenance_start_time` IS NULL
   AND `maintenance_end_time` IS NULL
   AND `maintenance_date` IS NOT NULL;

-- 4. 原维保日期放宽为可空（保留历史值，不再由业务写入）
ALTER TABLE `equipment_maintenance`
    MODIFY COLUMN `maintenance_date` DATE DEFAULT NULL COMMENT '维保日期（历史字段，已由维保开始/结束时间替代）';

-- 5. 组合索引：按设备 + 维保时间区间查询冲突记录
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'equipment_maintenance' AND INDEX_NAME = 'idx_em_equipment_time');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `equipment_maintenance` ADD INDEX `idx_em_equipment_time` (`equipment_id`, `maintenance_start_time`, `maintenance_end_time`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. 存量数据排查：维保结束时间不晚于开始时间（只查不改，人工确认后处理）
SELECT maintenance_id, equipment_id, maintenance_start_time, maintenance_end_time
  FROM equipment_maintenance
 WHERE is_deleted = 0
   AND maintenance_start_time IS NOT NULL
   AND maintenance_end_time IS NOT NULL
   AND maintenance_start_time >= maintenance_end_time;
