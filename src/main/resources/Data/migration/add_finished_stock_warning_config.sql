-- ===================================
-- 成品库存预警模块·默认配置项初始化
-- 1) 成品月均出库统计月数 avgMonths（默认 3）
-- 2) 成品库存预警月数 warningMonths（默认 3）
-- 3) 成品近效期天数 nearExpiryDays（默认 90）
-- 说明：INSERT IGNORE 幂等，已存在（含管理员修改过）的配置不会被覆盖
-- ===================================

INSERT IGNORE INTO `system_config`
  (`config_key`, `config_value`, `config_name`, `config_group`, `value_type`, `remark`, `status`, `is_deleted`, `version`)
VALUES
  ('dashboard.finishedStock.avgMonths',      '3',  '成品月均出库统计月数', '库存管理', 'int', '成品库存预警：自动计算月平均出库量的回溯完整自然月数（默认3）', 1, 0, 0),
  ('dashboard.finishedStock.warningMonths',  '3',  '成品库存预警月数',     '库存管理', 'int', '成品库存预警：可用库存可支撑月数低于该值时提醒生产（默认3）', 1, 0, 0),
  ('dashboard.finishedStock.nearExpiryDays', '90', '成品近效期天数',       '库存管理', 'int', '成品库存预警：有效期在该天数内的批次视为近效期，可选择剔除（默认90）', 1, 0, 0);
