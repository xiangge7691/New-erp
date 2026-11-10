-- ===================================
-- 日志模块·默认配置项初始化
-- 分组 config_group = 系统日志
-- 说明：INSERT IGNORE 幂等；已存在（含被管理员修改过）的配置不会被覆盖
-- ===================================

INSERT IGNORE INTO `system_config`
  (`config_key`, `config_value`, `config_name`, `config_group`, `value_type`, `remark`, `status`, `is_deleted`, `version`)
VALUES
  ('log.login.retention.days',     '180',  '登录日志保留天数', '系统日志', 'int',  '登录日志超过该天数后由定时任务物理删除（默认180天）', 1, 0, 0),
  ('log.operation.retention.days', '365',  '操作日志保留天数', '系统日志', 'int',  '操作日志超过该天数后由定时任务物理删除（默认365天）', 1, 0, 0),
  ('log.change.retention.days',    '180',  '数据变更日志保留天数', '系统日志', 'int', '数据变更日志超过该天数后由定时任务物理删除（默认180天）', 1, 0, 0),
  ('log.operation.enabled',        'true', '操作日志开关',     '系统日志', 'bool', '是否记录业务操作日志：true记录/false关闭（默认true）', 1, 0, 0);
