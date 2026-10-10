-- ===================================
-- 系统配置 config_group 语义升级：技术分组 → 业务模块
-- 1) 按配置键回填业务模块（库存管理/设备管理/人员管理/生产管理/质量管理/采购管理/制剂管理/系统管理）
-- 说明：幂等，可重复执行；仅在当前值仍为旧技术分组 dashboard_expiry 时改写，
--      避免覆盖管理员后续对归属的调整
-- ===================================

UPDATE `system_config` SET `config_group` = '库存管理' WHERE `config_key` = 'dashboard.expiry.stock.days'          AND `config_group` = 'dashboard_expiry';
UPDATE `system_config` SET `config_group` = '设备管理' WHERE `config_key` = 'dashboard.expiry.equipment.days'      AND `config_group` = 'dashboard_expiry';
UPDATE `system_config` SET `config_group` = '人员管理' WHERE `config_key` = 'dashboard.expiry.healthCert.days'     AND `config_group` = 'dashboard_expiry';
UPDATE `system_config` SET `config_group` = '人员管理' WHERE `config_key` = 'dashboard.expiry.personnelCert.days'  AND `config_group` = 'dashboard_expiry';
UPDATE `system_config` SET `config_group` = '人员管理' WHERE `config_key` = 'dashboard.expiry.training.days'       AND `config_group` = 'dashboard_expiry';
UPDATE `system_config` SET `config_group` = '生产管理' WHERE `config_key` = 'dashboard.expiry.disinfection.days'   AND `config_group` = 'dashboard_expiry';
UPDATE `system_config` SET `config_group` = '生产管理' WHERE `config_key` = 'dashboard.expiry.cleaning.days'       AND `config_group` = 'dashboard_expiry';
UPDATE `system_config` SET `config_group` = '质量管理' WHERE `config_key` = 'dashboard.expiry.verification.days'   AND `config_group` = 'dashboard_expiry';
UPDATE `system_config` SET `config_group` = '质量管理' WHERE `config_key` = 'dashboard.expiry.retainedSample.days' AND `config_group` = 'dashboard_expiry';
UPDATE `system_config` SET `config_group` = '采购管理' WHERE `config_key` = 'dashboard.expiry.supplierAudit.days'  AND `config_group` = 'dashboard_expiry';
UPDATE `system_config` SET `config_group` = '制剂管理' WHERE `config_key` = 'dashboard.expiry.approval.days'       AND `config_group` = 'dashboard_expiry';
UPDATE `system_config` SET `config_group` = '系统管理' WHERE `config_key` = 'dashboard.expiry.organization.days'   AND `config_group` = 'dashboard_expiry';
