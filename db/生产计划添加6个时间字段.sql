-- 生产计划添加6个时间字段（生产开始、生产结束、检验开始、检验结束、出库时间、归档时间）
ALTER TABLE production_plan
    ADD COLUMN `production_start_time` DATETIME DEFAULT NULL COMMENT '生产开始时间' AFTER `total_amount`,
    ADD COLUMN `production_end_time` DATETIME DEFAULT NULL COMMENT '生产结束时间' AFTER `production_start_time`,
    ADD COLUMN `inspection_start_time` DATETIME DEFAULT NULL COMMENT '检验开始时间' AFTER `production_end_time`,
    ADD COLUMN `inspection_end_time` DATETIME DEFAULT NULL COMMENT '检验结束时间' AFTER `inspection_start_time`,
    ADD COLUMN `outbound_time` DATETIME DEFAULT NULL COMMENT '出库时间' AFTER `inspection_end_time`,
    ADD COLUMN `archive_time` DATETIME DEFAULT NULL COMMENT '归档时间' AFTER `outbound_time`;
