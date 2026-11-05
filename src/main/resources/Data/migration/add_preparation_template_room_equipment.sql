-- ===================================
-- 制剂工序模板：新增配置室ID/设备ID并按ID回填存量数据
-- 1) preparation_process_template 新增 room_id、equipment_id 及索引（幂等）
-- 2) 存量数据回填：只回填能唯一确定的记录
-- 3) 输出无法唯一确定的残留清单，交业务人工确认
-- 4) 输出校验 SQL（要求全部返回 0 行）
-- 说明：本脚本仅用于测试库 8.134.48.54/erp_db，生产库暂不执行
-- ===================================

-- -----------------------------------
-- 1. 结构变更
-- -----------------------------------

-- 1.1 新增配置室ID
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preparation_process_template' AND COLUMN_NAME = 'room_id');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `preparation_process_template` ADD COLUMN `room_id` INT DEFAULT NULL COMMENT ''配置室ID（room_info.room_id）'' AFTER `process_type_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.2 新增设备ID
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preparation_process_template' AND COLUMN_NAME = 'equipment_id');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `preparation_process_template` ADD COLUMN `equipment_id` INT DEFAULT NULL COMMENT ''设备ID（equipment.equipment_id）'' AFTER `room_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.3 配置室索引
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preparation_process_template' AND INDEX_NAME = 'idx_ppt_room');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `preparation_process_template` ADD INDEX `idx_ppt_room` (`room_id`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.4 设备索引
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'preparation_process_template' AND INDEX_NAME = 'idx_ppt_equipment');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `preparation_process_template` ADD INDEX `idx_ppt_equipment` (`equipment_id`)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------
-- 2. 存量数据回填（只填能唯一确定的）
-- -----------------------------------

-- 2.1 房间：room_desc 存的是房间名称，room_name 在 room_info 中唯一
UPDATE `preparation_process_template` t
  JOIN `room_info` r ON r.`room_name` = TRIM(t.`room_desc`) AND r.`is_deleted` = 0
   SET t.`room_id` = r.`room_id`
 WHERE t.`is_deleted` = 0
   AND t.`room_id` IS NULL
   AND t.`room_desc` IS NOT NULL
   AND TRIM(t.`room_desc`) <> '';

-- 2.2 设备（模板已有房间）：同一房间内同名设备唯一才回填
UPDATE `preparation_process_template` t
  JOIN (
        SELECT e.`equipment_name`, e.`room_id`, MIN(e.`equipment_id`) AS `eid`, COUNT(*) AS `cnt`
          FROM `equipment` e
         WHERE e.`is_deleted` = 0 AND e.`room_id` IS NOT NULL
         GROUP BY e.`equipment_name`, e.`room_id`
        HAVING COUNT(*) = 1
       ) m ON m.`equipment_name` = TRIM(t.`equipment_desc`) AND m.`room_id` = t.`room_id`
   SET t.`equipment_id` = m.`eid`
 WHERE t.`is_deleted` = 0
   AND t.`equipment_id` IS NULL
   AND t.`room_id` IS NOT NULL;

-- 2.3 设备（模板无房间）：设备名称全局唯一才回填
UPDATE `preparation_process_template` t
  JOIN (
        SELECT e.`equipment_name`, MIN(e.`equipment_id`) AS `eid`, COUNT(*) AS `cnt`
          FROM `equipment` e
         WHERE e.`is_deleted` = 0
         GROUP BY e.`equipment_name`
        HAVING COUNT(*) = 1
       ) m ON m.`equipment_name` = TRIM(t.`equipment_desc`)
   SET t.`equipment_id` = m.`eid`
 WHERE t.`is_deleted` = 0
   AND t.`equipment_id` IS NULL
   AND t.`room_id` IS NULL;

-- 2.4 房间（设备已确定但模板无房间）：由设备所在房间带出
UPDATE `preparation_process_template` t
  JOIN `equipment` e ON e.`equipment_id` = t.`equipment_id` AND e.`is_deleted` = 0
   SET t.`room_id` = e.`room_id`
 WHERE t.`is_deleted` = 0
   AND t.`room_id` IS NULL
   AND e.`room_id` IS NOT NULL;

-- 2.5 快照文本与ID对齐（room_desc/equipment_desc 仅作展示，不再作为关联依据）
UPDATE `preparation_process_template` t
  JOIN `equipment` e ON e.`equipment_id` = t.`equipment_id` AND e.`is_deleted` = 0
   SET t.`equipment_desc` = e.`equipment_name`
 WHERE t.`is_deleted` = 0;

UPDATE `preparation_process_template` t
  JOIN `room_info` r ON r.`room_id` = t.`room_id` AND r.`is_deleted` = 0
   SET t.`room_desc` = r.`room_name`
 WHERE t.`is_deleted` = 0;

-- -----------------------------------
-- 3. 残留清单：无法唯一确定，需人工确认后补齐
-- -----------------------------------

SELECT t.`template_id`,
       t.`preparation_id`,
       p.`preparation_name`,
       t.`step_order`,
       t.`room_desc`,
       t.`equipment_desc`,
       CASE
           WHEN t.`equipment_id` IS NOT NULL AND t.`room_id` IS NULL THEN '设备未维护所在房间'
           WHEN t.`equipment_id` IS NULL AND t.`room_id` IS NULL THEN '无房间且设备名称无唯一匹配'
           WHEN NOT EXISTS (SELECT 1 FROM `equipment` e
                             WHERE e.`is_deleted` = 0 AND e.`equipment_name` = TRIM(t.`equipment_desc`)) THEN '设备名称无匹配'
           WHEN NOT EXISTS (SELECT 1 FROM `equipment` e
                             WHERE e.`is_deleted` = 0 AND e.`equipment_name` = TRIM(t.`equipment_desc`) AND e.`room_id` = t.`room_id`) THEN '设备不在该房间'
           ELSE '同房间同名设备多台，需按设备编码确认'
       END AS `reason`,
       (SELECT GROUP_CONCAT(CONCAT(e.`equipment_name`, '(', IFNULL(e.`fixed_asset_code`, '-'), ')@room=', IFNULL(e.`room_id`, 'NULL'))
                           ORDER BY e.`fixed_asset_code` SEPARATOR ' | ')
          FROM `equipment` e
         WHERE e.`is_deleted` = 0 AND e.`equipment_name` = TRIM(t.`equipment_desc`)) AS `candidates`
  FROM `preparation_process_template` t
  LEFT JOIN `preparation` p ON p.`preparation_id` = t.`preparation_id` AND p.`is_deleted` = 0
 WHERE t.`is_deleted` = 0
   AND (t.`equipment_id` IS NULL OR t.`room_id` IS NULL)
 ORDER BY t.`template_id`;

-- -----------------------------------
-- 4. 校验 SQL（要求全部返回 0 行）
-- -----------------------------------

-- 4.1 设备ID指向的设备不存在或已删除
SELECT t.`template_id` AS `orphan_equipment_template_id`, t.`equipment_id`
  FROM `preparation_process_template` t
  LEFT JOIN `equipment` e ON e.`equipment_id` = t.`equipment_id` AND e.`is_deleted` = 0
 WHERE t.`is_deleted` = 0 AND t.`equipment_id` IS NOT NULL AND e.`equipment_id` IS NULL;

-- 4.2 设备与模板房间不一致
SELECT t.`template_id` AS `mismatch_template_id`, t.`room_id`, e.`room_id` AS `equipment_room_id`
  FROM `preparation_process_template` t
  JOIN `equipment` e ON e.`equipment_id` = t.`equipment_id` AND e.`is_deleted` = 0
 WHERE t.`is_deleted` = 0
   AND t.`room_id` IS NOT NULL
   AND (e.`room_id` IS NULL OR e.`room_id` <> t.`room_id`);

-- 4.3 统计：已回填 / 待人工确认
SELECT COUNT(*) AS `total`,
       SUM(CASE WHEN `room_id` IS NOT NULL AND `equipment_id` IS NOT NULL THEN 1 ELSE 0 END) AS `resolved`,
       SUM(CASE WHEN `room_id` IS NULL OR `equipment_id` IS NULL THEN 1 ELSE 0 END) AS `pending`
  FROM `preparation_process_template`
 WHERE `is_deleted` = 0;
