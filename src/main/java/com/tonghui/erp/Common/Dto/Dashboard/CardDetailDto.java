package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;

import java.util.List;

/**
 * 卡片明细通用包装
 * <p>
 * 汇总值与明细一起返回：汇总值即卡片主数值对象（totals），
 * 明细为全量列表（details，不分页），前端直接渲染抽屉表格。
 * </p>
 *
 * @param <S> 汇总值类型（对应 {@link DashboardMetricsDto} 中的卡片 DTO）
 * @param <T> 明细行类型
 */
@Data
public class CardDetailDto<S, T> {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /**
     * 卡片标识：estimatedCost / actualCost / outputValue / totalPlan / delivery / pending
     */
    private String card;

    /**
     * 汇总值（与小计行一致，避免前端自行累加产生误差）
     */
    private S totals;

    /**
     * 明细列表（全量，不分页）
     */
    private List<T> details;

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 无参构造
     */
    public CardDetailDto() {
    }

    /**
     * 全参构造
     *
     * @param card    卡片标识
     * @param totals  汇总值
     * @param details 明细列表
     */
    public CardDetailDto(String card, S totals, List<T> details) {
        this.card = card;
        this.totals = totals;
        this.details = details;
    }

    // endregion
}
