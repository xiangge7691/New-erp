-- ===================================
-- 补全 file_operation_log 表中缺失的操作人信息
-- 执行前请先备份数据
-- ===================================

-- 1. 查看当前数据情况（执行前先确认）
SELECT COUNT(*) AS total_count FROM file_operation_log;
SELECT COUNT(*) AS null_user_name_count FROM file_operation_log WHERE user_name IS NULL OR user_name = '';
SELECT COUNT(*) AS null_user_id_count FROM file_operation_log WHERE user_id IS NULL OR user_id = 0;

-- 2. 补全 user_name 为空但 user_id 不为空的记录
UPDATE file_operation_log fol
INNER JOIN user u ON fol.user_id = u.user_id
SET fol.user_name = CASE
    WHEN u.user_name IS NOT NULL AND u.user_name != '' THEN u.user_name
    ELSE u.user_account
END
WHERE (fol.user_name IS NULL OR fol.user_name = '')
  AND fol.user_id IS NOT NULL
  AND fol.user_id > 0;

-- 3. 补全 user_id 为空或为0的记录，默认设置为超级管理员（user_id=1）
UPDATE file_operation_log fol
SET fol.user_id = 1,
    fol.user_name = COALESCE(
        (SELECT CASE
            WHEN u.user_name IS NOT NULL AND u.user_name != '' THEN u.user_name
            ELSE u.user_account
        END FROM user u WHERE u.user_id = 1),
        '超级管理员'
    )
WHERE fol.user_id IS NULL OR fol.user_id = 0;

-- 4. 验证结果
SELECT COUNT(*) AS remaining_null_count FROM file_operation_log WHERE user_name IS NULL OR user_name = '';
