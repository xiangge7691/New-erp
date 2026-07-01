-- 修复user表缺少的字段
ALTER TABLE `user` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已删除' AFTER `user_notes`;
ALTER TABLE `user` ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_deleted`;
