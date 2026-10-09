-- ===================================
-- 系统配置模块·默认配置项初始化
-- 分组 dashboard_expiry：首页/其他提醒的到期时间范围（天）
-- 说明：INSERT IGNORE 幂等；已存在（含被管理员修改过）的配置不会被覆盖
-- ===================================

INSERT IGNORE INTO `system_config`
  (`config_key`, `config_value`, `config_name`, `config_group`, `value_type`, `remark`, `status`, `is_deleted`, `version`)
VALUES
  ('dashboard.expiry.stock.days',          '30', '库存效期提醒天数',     'dashboard_expiry', 'int', '首页库存有效期临近/已过期的提醒范围（天）', 1, 0, 0),
  ('dashboard.expiry.equipment.days',      '30', '设备维保提醒天数',     'dashboard_expiry', 'int', '首页设备下次维保临近的提醒范围（天）', 1, 0, 0),
  ('dashboard.expiry.healthCert.days',     '30', '人员健康证提醒天数',   'dashboard_expiry', 'int', '首页人员健康证到期的提醒范围（天）', 1, 0, 0),
  ('dashboard.expiry.personnelCert.days',  '30', '人员证书提醒天数',     'dashboard_expiry', 'int', '首页人员证书到期的提醒范围（天）', 1, 0, 0),
  ('dashboard.expiry.disinfection.days',   '30', '环境消毒提醒天数',     'dashboard_expiry', 'int', '首页车间下次消毒临近的提醒范围（天）', 1, 0, 0),
  ('dashboard.expiry.organization.days',   '30', '机构证照提醒天数',     'dashboard_expiry', 'int', '首页机构许可证到期的提醒范围（天）', 1, 0, 0),
  ('dashboard.expiry.cleaning.days',       '30', '清洁提醒天数',         'dashboard_expiry', 'int', '首页/清洁提醒接口的清洁到期提醒范围（天）', 1, 0, 0),
  ('dashboard.expiry.supplierAudit.days',  '30', '供应商审核提醒天数',   'dashboard_expiry', 'int', '供应商审核到期提醒范围（天）', 1, 0, 0),
  ('dashboard.expiry.training.days',       '30', '培训提醒天数',         'dashboard_expiry', 'int', '培训到期提醒范围（天）', 1, 0, 0),
  ('dashboard.expiry.verification.days',   '30', '验证提醒天数',         'dashboard_expiry', 'int', '验证到期提醒范围（天）', 1, 0, 0),
  ('dashboard.expiry.retainedSample.days', '30', '留样到期提醒天数',     'dashboard_expiry', 'int', '首页留样期限临近/已到期的提醒范围（天）', 1, 0, 0),
  ('dashboard.expiry.approval.days',       '30', '批件过期提醒天数',     'dashboard_expiry', 'int', '首页制剂批件过期临近/已过期的提醒范围（天）', 1, 0, 0);
