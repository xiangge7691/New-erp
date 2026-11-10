-- ===================================
-- 工单工序执行记录：按开始/结束时间回填工序状态
-- 规则：
--   开始时间与结束时间齐全 → 已完成
--   仅有开始时间           → 执行中
--   均未填写               → 待执行
--   仅有结束时间（历史非法数据）→ 待执行（由保存校验拦截，不再产生）
-- 说明：后端保存/查询时已按相同口径推导，本脚本用于将存量数据落库对齐，
--       可按需执行；重复执行结果一致。
-- ===================================

UPDATE work_order_process_execution
SET status = CASE
        WHEN start_time IS NULL THEN '待执行'
        WHEN end_time IS NULL THEN '执行中'
        ELSE '已完成'
    END
WHERE is_deleted = 0;

-- 校验：回填后各状态数量
SELECT status, COUNT(*) AS 记录数
  FROM work_order_process_execution
 WHERE is_deleted = 0
 GROUP BY status;
