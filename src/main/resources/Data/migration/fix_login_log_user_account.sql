-- ===================================
-- 修正历史登录日志中被写成真实姓名的登录账号
-- 执行前请先备份数据
-- ===================================

-- 背景：
-- 登录响应中的 UserDto 由 MapStruct 转换生成，曾因缺失 userAccount -> userName 映射，
-- 使 UserDto.userName 被同名的 user_name（真实姓名）自动填充。
-- AuthController 把该字段作为登录账号写入登录日志，
-- 于是 user_account 与 user_name 都变成了"超级管理员"这类姓名。

-- 1. 查看受影响的记录（执行前先确认）
SELECT l.id, l.user_id, l.user_account, l.user_name, u.user_account AS expected_account
FROM sys_login_log l
INNER JOIN `user` u ON l.user_id = u.user_id
WHERE l.user_account = u.user_name
  AND l.user_account <> u.user_account
ORDER BY l.id DESC;

-- 2. 按用户ID回填真实登录账号
--    仅修正：能关联到用户、且当前值与用户姓名相同的记录；
--    登录失败记录（user_id 为空）保存的是用户当时输入的账号，不在修正范围内。
UPDATE sys_login_log l
INNER JOIN `user` u ON l.user_id = u.user_id
SET l.user_account = u.user_account
WHERE l.user_account = u.user_name
  AND l.user_account <> u.user_account;

-- 3. 验证结果：应返回 0
SELECT COUNT(*) AS remaining_mismatch_count
FROM sys_login_log l
INNER JOIN `user` u ON l.user_id = u.user_id
WHERE l.user_account = u.user_name
  AND l.user_account <> u.user_account;
