package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Common.Dto.Production.ProductionDurationSummaryDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 生产用时计算工具类单元测试
 * <p>
 * 覆盖小时数计算、无效时间过滤及按制剂平均生产时间汇总等场景，不依赖数据库
 * </p>
 */
public class ProductionDurationUtilsTest {

    // region 生产用时计算
    // ===================================
    // 生产用时计算
    // ===================================

    /**
     * 正常时间区间按秒精确换算为小时并保留两位小数
     */
    @Test
    public void testCalcHours() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 2, 12, 0);

        assertEquals(new BigDecimal("36.00"), ProductionDurationUtils.calcHours(start, end));
    }

    /**
     * 开始或结束为空、结束早于开始时返回 null
     */
    @Test
    public void testCalcHoursInvalid() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 2, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 0, 0);

        assertNull(ProductionDurationUtils.calcHours(null, end));
        assertNull(ProductionDurationUtils.calcHours(start, null));
        assertNull(ProductionDurationUtils.calcHours(start, end));
    }

    // endregion

    // region 平均生产时间汇总
    // ===================================
    // 平均生产时间汇总
    // ===================================

    /**
     * 总体平均值与按制剂平均值均按有效样本计算
     */
    @Test
    public void testSummarize() {
        List<ProductionDurationUtils.DurationSample> samples = List.of(
                new ProductionDurationUtils.DurationSample(1L, "ZJ001", "制剂A", new BigDecimal("2.00")),
                new ProductionDurationUtils.DurationSample(1L, "ZJ001", "制剂A", new BigDecimal("4.00")),
                new ProductionDurationUtils.DurationSample(2L, "ZJ002", "制剂B", new BigDecimal("3.00")));

        ProductionDurationSummaryDto summary = ProductionDurationUtils.summarize(samples);

        assertEquals(3, summary.getSampleCount());
        assertEquals(new BigDecimal("3.00"), summary.getAvgHours());
        assertEquals(2, summary.getByPreparation().size());
        assertEquals(new BigDecimal("3.00"), summary.getByPreparation().get(0).getAvgHours());
        assertEquals(2, summary.getByPreparation().get(0).getSampleCount());
    }

    /**
     * 空样本返回数量 0、平均值为空
     */
    @Test
    public void testSummarizeEmpty() {
        ProductionDurationSummaryDto summary = ProductionDurationUtils.summarize(List.of());

        assertEquals(0, summary.getSampleCount());
        assertNull(summary.getAvgHours());
        assertEquals(0, summary.getByPreparation().size());
    }

    // endregion
}
