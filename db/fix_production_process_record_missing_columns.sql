-- ============================================================
-- 修复 production_process_record 表缺少 is_deleted 和 version 列
-- 问题：Entity 声明了 isDeleted 和 version 字段，但数据库表缺少这两列
-- ============================================================

ALTER TABLE `production_process_record` 
  ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `remark`,
  ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;
