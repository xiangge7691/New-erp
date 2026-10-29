-- ===================================
-- 工单工序执行记录：设备使用冲突校验配套脚本
-- 1) 增加 (equipment_id, start_time, end_time) 组合索引，支撑冲突查询与按设备导出
-- 2) 历史脏数据：前端未选设备时写入的 equipment_id = 0 统一置为 NULL
-- 3) 存量非法时间与重叠数据排查（只查不改，人工确认后处理）
-- 说明：索引不幂等，请在目标库执行前先确认索引是否已存在。
-- ===================================

-- 1. 组合索引：按设备查询在时间窗内的占用记录
ALTER TABLE work_order_process_execution
    ADD INDEX idx_wpe_equipment_time (equipment_id, start_time, end_time);

-- 1.1 组合索引：按设备 + 维保日期查询维保冲突记录
ALTER TABLE equipment_maintenance
    ADD INDEX idx_em_equipment_date (equipment_id, maintenance_date);

-- 2. equipment_id = 0 视为"未选设备"，统一改为 NULL
UPDATE work_order_process_execution
   SET equipment_id = NULL
 WHERE equipment_id = 0;

-- 3.1 存量非法时间：结束时间不晚于开始时间
SELECT id, work_order_id, equipment_id, start_time, end_time
  FROM work_order_process_execution
 WHERE is_deleted = 0
   AND start_time IS NOT NULL
   AND end_time IS NOT NULL
   AND start_time >= end_time;

-- 3.2 存量同设备时间重叠（自连接，结果需业务确认后人工修正）
SELECT a.id            AS 记录A,
       b.id            AS 记录B,
       a.equipment_id  AS 设备ID,
       a.start_time    AS A开始,
       a.end_time      AS A结束,
       b.start_time    AS B开始,
       b.end_time      AS B结束
  FROM work_order_process_execution a
  JOIN work_order_process_execution b
    ON a.equipment_id = b.equipment_id
   AND a.id < b.id
   AND a.is_deleted = 0
   AND b.is_deleted = 0
   AND a.equipment_id IS NOT NULL
   AND a.start_time IS NOT NULL
   AND a.end_time IS NOT NULL
   AND b.start_time IS NOT NULL
   AND b.end_time IS NOT NULL
   AND a.start_time < b.end_time
   AND a.end_time > b.start_time;
