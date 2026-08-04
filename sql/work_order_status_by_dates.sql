-- ===================================
-- 工单状态流转支持脚本
-- 状态规则（由 Service 依据日期字段自动计算）：
--   待生产 - 配置日期(config_date)为空
--   生产中 - 配置日期有值 且 配置完成日期(config_complete_time)为空
--   已生产 - 配置完成日期有值
--   已归档 - 归档时间(archive_time)有值
-- 说明：第1步仅当 archive_time 列不存在时执行（MySQL 不支持 ADD COLUMN IF NOT EXISTS，需先查询确认）
-- ===================================

-- 第1步（可选）：确认 work_order 表是否缺少 archive_time 列
SELECT COLUMN_NAME FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'erp_db' AND TABLE_NAME = 'work_order' AND COLUMN_NAME = 'archive_time';

-- 若上一步无结果，则执行以下语句新增归档时间列
-- ALTER TABLE `work_order` ADD COLUMN `archive_time` datetime NULL COMMENT '归档时间' AFTER `config_complete_time`;

-- 第2步（可选）：回填存量工单状态（仅在状态列为空或为旧值时执行）
UPDATE `work_order`
SET `current_status` = CASE
    WHEN `archive_time` IS NOT NULL THEN '已归档'
    WHEN `config_complete_time` IS NOT NULL THEN '已生产'
    WHEN `config_date` IS NOT NULL THEN '生产中'
    ELSE '待生产'
END
WHERE `current_status` IS NULL OR `current_status` NOT IN ('待生产', '生产中', '已生产', '已归档');
