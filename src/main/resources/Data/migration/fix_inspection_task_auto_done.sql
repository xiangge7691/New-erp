-- ===================================
-- 修复检验任务被自动置为“已完成”的存量数据（幂等）
-- 背景：旧逻辑在检验记录/留样完成后，按 retention_required / destruction_required
--       自动跳过留样/销毁并置 DONE，导致留样、销毁环节无法补办和上传附件。
-- 适用：部署修复代码后，将历史错误完成的任务退回留样或销毁环节。
-- 注意：
--   1. 执行前请先备份 inspection_task、inspection_task_step；
--   2. 先执行“预览”确认范围，必要时在临时表 WHERE 中追加时间或任务ID限制；
--   3. 本脚本不修改 retention_required / destruction_required 业务标志；
--      若业务要求所有品类都留样，请另行确认后再调整标志位。
-- ===================================

-- 1. 预览受影响任务
SELECT id, task_code, item_category, retention_required, destruction_required,
       retention_ref_id, destruction_ref_id, current_step, task_status, updated_time
FROM inspection_task
WHERE is_deleted = 0
  AND current_step = 'DONE'
  AND task_status = '已完成'
  AND (retention_ref_id IS NULL
       OR (destruction_required = 1 AND destruction_ref_id IS NULL))
ORDER BY id;

-- 2. 收集待回退任务（按需追加时间或任务ID限制）
--    只回退“要求留样但漏掉”的任务时，在下方 WHERE 中追加：AND retention_required = 1
--    限定时间范围时，追加：AND updated_time >= '2026-01-01'
DROP TEMPORARY TABLE IF EXISTS tmp_inspection_task_reopen;
CREATE TEMPORARY TABLE tmp_inspection_task_reopen AS
SELECT id,
       CASE
         WHEN retention_ref_id IS NULL THEN 'RETENTION'
         ELSE 'DESTRUCTION'
       END AS target_step
FROM inspection_task
WHERE is_deleted = 0
  AND current_step = 'DONE'
  AND task_status = '已完成'
  AND (retention_ref_id IS NULL
       OR (destruction_required = 1 AND destruction_ref_id IS NULL));

-- 3. 任务退回留样/销毁
UPDATE inspection_task t
JOIN tmp_inspection_task_reopen r ON r.id = t.id
SET t.current_step = r.target_step,
    t.task_status = '进行中',
    t.complete_time = NULL,
    t.updated_time = NOW(),
    t.version = t.version + 1
WHERE t.is_deleted = 0;

-- 4. 同步步骤状态
-- 4.1 退回留样的任务：留样进行中，销毁回到待办理
UPDATE inspection_task_step s
JOIN tmp_inspection_task_reopen r ON r.id = s.task_id
SET s.step_status = CASE
      WHEN s.step_code = 'RETENTION' THEN '进行中'
      WHEN s.step_code = 'DESTRUCTION' THEN '待办理'
      ELSE s.step_status END,
    s.complete_time = CASE
      WHEN s.step_code IN ('RETENTION', 'DESTRUCTION') THEN NULL
      ELSE s.complete_time END,
    s.skip_reason = CASE
      WHEN s.step_code IN ('RETENTION', 'DESTRUCTION') THEN NULL
      ELSE s.skip_reason END,
    s.start_time = CASE
      WHEN s.step_code = 'RETENTION' AND s.start_time IS NULL THEN NOW()
      ELSE s.start_time END,
    s.updated_time = NOW(),
    s.version = s.version + 1
WHERE r.target_step = 'RETENTION'
  AND s.is_deleted = 0
  AND s.step_code IN ('RETENTION', 'DESTRUCTION');

-- 4.2 退回销毁的任务：销毁进行中
UPDATE inspection_task_step s
JOIN tmp_inspection_task_reopen r ON r.id = s.task_id
SET s.step_status = '进行中',
    s.complete_time = NULL,
    s.skip_reason = NULL,
    s.start_time = COALESCE(s.start_time, NOW()),
    s.updated_time = NOW(),
    s.version = s.version + 1
WHERE r.target_step = 'DESTRUCTION'
  AND s.is_deleted = 0
  AND s.step_code = 'DESTRUCTION';

-- 5. 校验：回退后不应再有“已完成但缺留样/销毁”的记录
SELECT id, task_code, current_step, task_status, retention_ref_id, destruction_ref_id
FROM inspection_task
WHERE is_deleted = 0
  AND current_step = 'DONE'
  AND task_status = '已完成'
  AND (retention_ref_id IS NULL
       OR (destruction_required = 1 AND destruction_ref_id IS NULL));

DROP TEMPORARY TABLE IF EXISTS tmp_inspection_task_reopen;
