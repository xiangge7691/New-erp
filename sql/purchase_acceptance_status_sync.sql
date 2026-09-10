-- ============================================================
-- 采购订单与货物验收单状态对账修复
-- 背景：验收单状态变更未实时同步采购订单（仅确认到货与合格入库两处同步，
--       且合格入库误写为"已完成"，不在状态字典内）。
-- 规则：以采购订单关联的"最新活动验收单"（非已退换，取 acceptance_id 最大）状态为准，
--       将采购订单状态回写为同名状态（运输中/到货初验/物料检验/待退货/已入库）。
--       手工终态"已关闭"的采购订单不覆盖。
-- 执行：mysql -h8.134.48.54 -uerp_db -p89749050 erp_db < sql/purchase_acceptance_status_sync.sql
-- 验证：SELECT po.purchase_number, po.status, a.status AS acc_status
--       FROM purchase_orders po
--       LEFT JOIN acceptance_order a ON a.purchase_number = po.purchase_number AND a.is_deleted=0
--       ORDER BY po.id;
-- ============================================================

UPDATE purchase_orders po
JOIN (
    -- 每张采购订单的最新活动验收单（状态可映射，排除已退换终态）
    SELECT a.purchase_number, a.status
    FROM acceptance_order a
    JOIN (
        SELECT purchase_number, MAX(acceptance_id) AS max_id
        FROM acceptance_order
        WHERE is_deleted = 0
          AND status IN ('运输中', '到货初验', '物料检验', '待退货', '已入库')
        GROUP BY purchase_number
    ) t ON t.max_id = a.acceptance_id
) act ON act.purchase_number = po.purchase_number
SET po.status = act.status
WHERE po.status <> act.status
  AND po.status <> '已关闭';