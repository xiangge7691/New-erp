package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Dashboard.DashboardTreeDto;

/**
 * 首页明细下钻服务（月份 → 制剂 → 批次）
 * <p>
 * 复用成本核算的三份卡片明细（预估产值 / 预估成本 / 实际成本），
 * 按「月份 → 制剂 → 批次」重新聚合为一棵树，避免前端多次请求自行 join。
 * </p>
 * <p>
 * 各层级合计与该月份卡片数值一致：
 * 预估产值、预估成本、交付量按交付月归集；实际成本按成本发生月归集。
 * </p>
 */
public interface DashboardTreeService {

    /**
     * 成本与生产明细树
     *
     * 示例请求：
     * GET /api/dashboard/details/tree?startMonth=2026-08&endMonth=2026-10
     *
     * @param startMonth 起始月份（yyyy-MM，可空）
     * @param endMonth   结束月份（yyyy-MM，可空）
     * @return 明细树：区间合计 + 月份节点 + 制剂节点 + 批次节点
     */
    DashboardTreeDto getDetailTree(String startMonth, String endMonth);
}
