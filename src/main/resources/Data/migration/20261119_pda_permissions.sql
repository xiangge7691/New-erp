-- ===================================
-- PDA 移动端权限、专用角色、专用用户
-- 测试库：192.168.5.100 / erp_db
-- 说明：只做菜单/前端路由权限，不做接口级权限校验
-- 账号密码：pda123（Argon2）
-- ===================================

-- 一、菜单权限
INSERT INTO `permission` (`perm_key`, `perm_name`, `perm_type`, `parent_id`, `display_order`,
                          `perm_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('pda', '移动作业', 'menu', NULL, 8, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `perm_name` = VALUES(`perm_name`), `perm_type` = VALUES(`perm_type`),
                        `parent_id` = VALUES(`parent_id`), `display_order` = VALUES(`display_order`),
                        `perm_status` = 1, `is_deleted` = 0, `updated_time` = NOW();
SET @pda_id := (SELECT `perm_id` FROM `permission` WHERE `perm_key` = 'pda' LIMIT 1);

INSERT INTO `permission` (`perm_key`, `perm_name`, `perm_type`, `parent_id`, `display_order`,
                          `perm_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('pda_stock', '仓储扫码', 'menu', @pda_id, 0, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `perm_name` = VALUES(`perm_name`), `parent_id` = VALUES(`parent_id`),
                        `display_order` = VALUES(`display_order`), `perm_status` = 1,
                        `is_deleted` = 0, `updated_time` = NOW();
SET @pda_stock_id := (SELECT `perm_id` FROM `permission` WHERE `perm_key` = 'pda_stock' LIMIT 1);

INSERT INTO `permission` (`perm_key`, `perm_name`, `perm_type`, `parent_id`, `display_order`,
                          `perm_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('pda_stock_task', '入库/出库/退库任务', 'menu', @pda_stock_id, 0, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `perm_name` = VALUES(`perm_name`), `parent_id` = VALUES(`parent_id`),
                        `display_order` = VALUES(`display_order`), `perm_status` = 1,
                        `is_deleted` = 0, `updated_time` = NOW();

INSERT INTO `permission` (`perm_key`, `perm_name`, `perm_type`, `parent_id`, `display_order`,
                          `perm_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('pda_stock_reprint', '补打标签', 'menu', @pda_stock_id, 1, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `perm_name` = VALUES(`perm_name`), `parent_id` = VALUES(`parent_id`),
                        `display_order` = VALUES(`display_order`), `perm_status` = 1,
                        `is_deleted` = 0, `updated_time` = NOW();

INSERT INTO `permission` (`perm_key`, `perm_name`, `perm_type`, `parent_id`, `display_order`,
                          `perm_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('pda_stock_records', '作业记录', 'menu', @pda_stock_id, 2, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `perm_name` = VALUES(`perm_name`), `parent_id` = VALUES(`parent_id`),
                        `display_order` = VALUES(`display_order`), `perm_status` = 1,
                        `is_deleted` = 0, `updated_time` = NOW();

INSERT INTO `permission` (`perm_key`, `perm_name`, `perm_type`, `parent_id`, `display_order`,
                          `perm_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('pda_production', '生产工序执行', 'menu', @pda_id, 1, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `perm_name` = VALUES(`perm_name`), `parent_id` = VALUES(`parent_id`),
                        `display_order` = VALUES(`display_order`), `perm_status` = 1,
                        `is_deleted` = 0, `updated_time` = NOW();
SET @pda_prod_id := (SELECT `perm_id` FROM `permission` WHERE `perm_key` = 'pda_production' LIMIT 1);

INSERT INTO `permission` (`perm_key`, `perm_name`, `perm_type`, `parent_id`, `display_order`,
                          `perm_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('pda_production_task', '生产任务', 'menu', @pda_prod_id, 0, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `perm_name` = VALUES(`perm_name`), `parent_id` = VALUES(`parent_id`),
                        `display_order` = VALUES(`display_order`), `perm_status` = 1,
                        `is_deleted` = 0, `updated_time` = NOW();

INSERT INTO `permission` (`perm_key`, `perm_name`, `perm_type`, `parent_id`, `display_order`,
                          `perm_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('pda_production_records', '提交记录', 'menu', @pda_prod_id, 1, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `perm_name` = VALUES(`perm_name`), `parent_id` = VALUES(`parent_id`),
                        `display_order` = VALUES(`display_order`), `perm_status` = 1,
                        `is_deleted` = 0, `updated_time` = NOW();

-- 二、专用角色
INSERT INTO `role` (`role_name`, `role_desc`, `role_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('PDA-仓储库管', 'PDA移动作业：仓储扫码、补打标签、作业记录', 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `role_desc` = VALUES(`role_desc`), `role_status` = 1, `is_deleted` = 0, `updated_time` = NOW();
SET @role_wh := (SELECT `role_id` FROM `role` WHERE `role_name` = 'PDA-仓储库管' LIMIT 1);

INSERT INTO `role` (`role_name`, `role_desc`, `role_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('PDA-生产操作工', 'PDA移动作业：生产任务、工序执行、提交记录', 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `role_desc` = VALUES(`role_desc`), `role_status` = 1, `is_deleted` = 0, `updated_time` = NOW();
SET @role_op := (SELECT `role_id` FROM `role` WHERE `role_name` = 'PDA-生产操作工' LIMIT 1);

INSERT INTO `role` (`role_name`, `role_desc`, `role_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('PDA-生产班组长', 'PDA移动作业：生产任务、工序执行、提交记录（可代录/补录）', 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `role_desc` = VALUES(`role_desc`), `role_status` = 1, `is_deleted` = 0, `updated_time` = NOW();
SET @role_lead := (SELECT `role_id` FROM `role` WHERE `role_name` = 'PDA-生产班组长' LIMIT 1);

-- 三、角色权限
INSERT INTO `role_perm` (`role_id`, `perm_id`, `created_time`, `is_deleted`)
SELECT @role_wh, `perm_id`, NOW(), 0 FROM `permission`
WHERE `perm_key` IN ('pda', 'pda_stock', 'pda_stock_task', 'pda_stock_reprint', 'pda_stock_records')
  AND NOT EXISTS (SELECT 1 FROM `role_perm` rp WHERE rp.`role_id` = @role_wh AND rp.`perm_id` = `permission`.`perm_id` AND rp.`is_deleted` = 0);

INSERT INTO `role_perm` (`role_id`, `perm_id`, `created_time`, `is_deleted`)
SELECT @role_op, `perm_id`, NOW(), 0 FROM `permission`
WHERE `perm_key` IN ('pda', 'pda_production', 'pda_production_task', 'pda_production_records')
  AND NOT EXISTS (SELECT 1 FROM `role_perm` rp WHERE rp.`role_id` = @role_op AND rp.`perm_id` = `permission`.`perm_id` AND rp.`is_deleted` = 0);

INSERT INTO `role_perm` (`role_id`, `perm_id`, `created_time`, `is_deleted`)
SELECT @role_lead, `perm_id`, NOW(), 0 FROM `permission`
WHERE `perm_key` IN ('pda', 'pda_production', 'pda_production_task', 'pda_production_records')
  AND NOT EXISTS (SELECT 1 FROM `role_perm` rp WHERE rp.`role_id` = @role_lead AND rp.`perm_id` = `permission`.`perm_id` AND rp.`is_deleted` = 0);

-- 四、专用用户（密码 pda123）
INSERT INTO `user` (`user_account`, `user_name`, `password`, `gender`, `user_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('pda_wh', 'PDA仓储库管', 't44xwK/BGeaCNwyqcMngK8PtVWpGvT+URQZLA+JHhCeLqpvqR6wW/XwyXwcYSz/3', 'OTHER', 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `user_name` = VALUES(`user_name`), `user_status` = 1, `is_deleted` = 0, `updated_time` = NOW();
SET @u_wh := (SELECT `user_id` FROM `user` WHERE `user_account` = 'pda_wh' LIMIT 1);

INSERT INTO `user` (`user_account`, `user_name`, `password`, `gender`, `user_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('pda_op', 'PDA生产操作工', 't44xwK/BGeaCNwyqcMngK8PtVWpGvT+URQZLA+JHhCeLqpvqR6wW/XwyXwcYSz/3', 'OTHER', 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `user_name` = VALUES(`user_name`), `user_status` = 1, `is_deleted` = 0, `updated_time` = NOW();
SET @u_op := (SELECT `user_id` FROM `user` WHERE `user_account` = 'pda_op' LIMIT 1);

INSERT INTO `user` (`user_account`, `user_name`, `password`, `gender`, `user_status`, `is_deleted`, `version`, `created_time`, `updated_time`)
VALUES ('pda_lead', 'PDA生产班组长', 't44xwK/BGeaCNwyqcMngK8PtVWpGvT+URQZLA+JHhCeLqpvqR6wW/XwyXwcYSz/3', 'OTHER', 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `user_name` = VALUES(`user_name`), `user_status` = 1, `is_deleted` = 0, `updated_time` = NOW();
SET @u_lead := (SELECT `user_id` FROM `user` WHERE `user_account` = 'pda_lead' LIMIT 1);

-- 五、用户角色
INSERT INTO `user_role` (`user_id`, `role_id`, `created_time`, `is_deleted`, `version`)
SELECT @u_wh, @role_wh, NOW(), 0, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user_role` WHERE `user_id` = @u_wh AND `role_id` = @role_wh AND `is_deleted` = 0);

INSERT INTO `user_role` (`user_id`, `role_id`, `created_time`, `is_deleted`, `version`)
SELECT @u_op, @role_op, NOW(), 0, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user_role` WHERE `user_id` = @u_op AND `role_id` = @role_op AND `is_deleted` = 0);

INSERT INTO `user_role` (`user_id`, `role_id`, `created_time`, `is_deleted`, `version`)
SELECT @u_lead, @role_lead, NOW(), 0, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `user_role` WHERE `user_id` = @u_lead AND `role_id` = @role_lead AND `is_deleted` = 0);
