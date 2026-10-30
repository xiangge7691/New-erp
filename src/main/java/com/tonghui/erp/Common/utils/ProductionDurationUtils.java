package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Common.Dto.Production.PreparationDurationStatDto;
import com.tonghui.erp.Common.Dto.Production.ProductionDurationSummaryDto;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 生产用时计算工具类
 * <p>
 * 统一生产计划、生产任务的生产用时口径：开始时间到结束时间的天数，
 * 并支持按制剂维度汇总平均生产时间
 * </p>
 */
public final class ProductionDurationUtils {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    /** 一天的秒数 */
    private static final BigDecimal SECONDS_PER_DAY = BigDecimal.valueOf(86400);

    /** 平均天数保留小数位 */
    private static final int AVG_SCALE = 2;

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 私有构造方法，禁止实例化工具类
     */
    private ProductionDurationUtils() {
    }

    // endregion

    // region 生产用时计算
    // ===================================
    // 生产用时计算
    // ===================================

    /**
     * 计算两个时间之间的生产用时（天）
     * <p>
     * 开始或结束时间为空、结束时间早于开始时间时返回 null
     * </p>
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 生产用时（天，保留两位小数），无效时返回 null
     */
    public static BigDecimal calcDays(LocalDateTime start, LocalDateTime end) {
        if (!isValid(start, end)) {
            return null;
        }
        long seconds = Duration.between(start, end).getSeconds();
        return BigDecimal.valueOf(seconds)
                .divide(SECONDS_PER_DAY, AVG_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 判断生产用时是否有效
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 两个时间均非空且结束时间不早于开始时间返回 true
     */
    public static boolean isValid(LocalDateTime start, LocalDateTime end) {
        return start != null && end != null && !end.isBefore(start);
    }

    // endregion

    // region 平均生产时间统计
    // ===================================
    // 平均生产时间统计
    // ===================================

    /**
     * 汇总平均生产时间
     * <p>
     * 总体平均值按有效样本加权计算；制剂维度按 preparationId 分组，
     * 缺失时依次按制剂编码、制剂名称兜底
     * </p>
     *
     * @param samples 生产用时样本列表
     * @return 汇总结果（无有效样本时 sampleCount=0、avgDays=null）
     */
    public static ProductionDurationSummaryDto summarize(List<DurationSample> samples) {
        ProductionDurationSummaryDto summary = new ProductionDurationSummaryDto();
        if (samples == null || samples.isEmpty()) {
            return summary;
        }

        // 制剂分组累加器
        Map<String, DurationAccumulator> accumulatorMap = new LinkedHashMap<>();
        BigDecimal totalDays = BigDecimal.ZERO;
        long totalCount = 0;

        for (DurationSample sample : samples) {
            if (sample == null || sample.days() == null || sample.days().signum() < 0) {
                continue;
            }
            totalDays = totalDays.add(sample.days());
            totalCount++;

            String key = buildPreparationKey(sample);
            DurationAccumulator accumulator = accumulatorMap.computeIfAbsent(key, k -> {
                DurationAccumulator item = new DurationAccumulator();
                item.preparationId = sample.preparationId();
                item.preparationCode = sample.preparationCode();
                item.preparationName = sample.preparationName();
                return item;
            });
            accumulator.totalDays = accumulator.totalDays.add(sample.days());
            accumulator.sampleCount++;
        }

        summary.setSampleCount(totalCount);
        if (totalCount > 0) {
            summary.setAvgDays(totalDays.divide(BigDecimal.valueOf(totalCount), AVG_SCALE, RoundingMode.HALF_UP));
        }

        List<PreparationDurationStatDto> byPreparation = new ArrayList<>();
        for (DurationAccumulator accumulator : accumulatorMap.values()) {
            PreparationDurationStatDto item = new PreparationDurationStatDto();
            item.setPreparationId(accumulator.preparationId);
            item.setPreparationCode(accumulator.preparationCode);
            item.setPreparationName(accumulator.preparationName);
            item.setSampleCount(accumulator.sampleCount);
            item.setAvgDays(accumulator.totalDays.divide(
                    BigDecimal.valueOf(accumulator.sampleCount), AVG_SCALE, RoundingMode.HALF_UP));
            byPreparation.add(item);
        }
        byPreparation.sort(Comparator
                .comparing(PreparationDurationStatDto::getPreparationName,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PreparationDurationStatDto::getPreparationCode,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        summary.setByPreparation(byPreparation);

        return summary;
    }

    /**
     * 构建制剂分组键
     *
     * @param sample 生产用时样本
     * @return 分组键
     */
    private static String buildPreparationKey(DurationSample sample) {
        if (sample.preparationId() != null) {
            return "ID:" + sample.preparationId();
        }
        if (StringUtils.hasText(sample.preparationCode())) {
            return "CODE:" + sample.preparationCode();
        }
        if (StringUtils.hasText(sample.preparationName())) {
            return "NAME:" + sample.preparationName();
        }
        return "UNKNOWN";
    }

    // endregion

    // region 内部数据结构
    // ===================================
    // 内部数据结构
    // ===================================

    /**
     * 生产用时样本
     *
     * @param preparationId   制剂ID
     * @param preparationCode 制剂编码
     * @param preparationName 制剂名称
     * @param days            生产用时（天）
     */
    public record DurationSample(Long preparationId, String preparationCode,
                                 String preparationName, BigDecimal days) {
    }

    /**
     * 制剂平均生产时间累加器
     */
    private static class DurationAccumulator {

        /** 制剂ID */
        private Long preparationId;

        /** 制剂编码 */
        private String preparationCode;

        /** 制剂名称 */
        private String preparationName;

        /** 生产用时合计 */
        private BigDecimal totalDays = BigDecimal.ZERO;

        /** 样本数 */
        private long sampleCount;
    }

    // endregion
}
