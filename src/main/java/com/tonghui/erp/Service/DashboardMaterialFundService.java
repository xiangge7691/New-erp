package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Dashboard.ChartDataDto;
import com.tonghui.erp.Common.Dto.Dashboard.DashboardMaterialFundDetailDto;

/**
 * 首页原辅包资金统计服务
 * <p>
 * 负责成本核算分析区的原辅包资金三项图表（入库 / 出库净额 / 当前库存）、
 * 以及点击色块后的物料级明细下钻。
 * </p>
 * <p>
 * 口径：
 * <ul>
 *   <li>入库：入库明细金额，按入库单日期归月，状态「已入库」</li>
 *   <li>出库净额：出库明细金额 − 退库冲减，按出库单日期归月，状态「已出库」</li>
 *   <li>库存：当前时点快照，数量 × 单价，忽略月份筛选</li>
 *   <li>分类仅统计 原料 / 辅料 / 包材</li>
 * </ul>
 * </p>
 */
public interface DashboardMaterialFundService {

    // region 图表数据
    // ===================================
    // 图表数据
    // ===================================

    /**
     * 原辅包资金三项图表
     *
     * @param startMonth 起始月份（yyyy-MM，可空）
     * @param endMonth   结束月份（yyyy-MM，可空）
     * @return 入库 / 出库净额 / 当前库存
     */
    ChartDataDto.MaterialFunds getMaterialFunds(String startMonth, String endMonth);

    // endregion

    // region 物料级下钻
    // ===================================
    // 物料级下钻
    // ===================================

    /**
     * 物料级明细（全量，不分页）
     *
     * 示例请求：
     * GET /api/dashboard/material-funds/details?type=outbound&category=原料&month=2026-08
     *
     * @param type     类型：inbound / outbound / stock
     * @param category 分类：原料 / 辅料 / 包材，可空表示全部
     * @param month    月份（yyyy-MM，仅 inbound/outbound 生效）
     * @return 物料级明细
     */
    DashboardMaterialFundDetailDto getMaterialFundDetails(String type, String category, String month);

    // endregion
}
