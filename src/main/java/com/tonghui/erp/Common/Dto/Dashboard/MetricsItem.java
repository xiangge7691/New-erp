package com.tonghui.erp.Common.Dto.Dashboard;

import lombok.Data;
import java.util.List;

/**
 * 指标包装项
 * <p>
 * 将汇总数字与其对应的明细记录列表打包返回，
 * 前端可同时展示"数字卡片"和"展开明细"
 * </p>
 *
 * @param <S> 汇总值类型（Double / Long）
 * @param <D> 明细记录类型
 */
@Data
public class MetricsItem<S, D> {

    // region 数据字段
    // ===================================
    // 数据字段
    // ===================================

    /**
     * 汇总值（预估产值/订单量/交付量/采购额/待生产数）
     */
    private S summary;

    /**
     * 明细记录列表（全量）
     */
    private List<D> details;

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 无参构造
     */
    public MetricsItem() {
    }

    /**
     * 全参构造
     *
     * @param summary 汇总值
     * @param details 明细列表
     */
    public MetricsItem(S summary, List<D> details) {
        this.summary = summary;
        this.details = details;
    }

    // endregion
}
