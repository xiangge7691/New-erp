package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.FinishedStock.FinishedOutboundMonthlyDto;
import com.tonghui.erp.Common.Dto.FinishedStock.FinishedStockAggDto;
import com.tonghui.erp.Common.Dto.FinishedStock.FinishedStockWarningDto;
import com.tonghui.erp.Common.Dto.FinishedStock.FinishedStockWarningSummaryDto;
import com.tonghui.erp.Common.Dto.FinishedStock.MonthlyOutboundDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.mapper.PreparationMapper;
import com.tonghui.erp.Data.mapper.SalesOrderMapper;
import com.tonghui.erp.Data.mapper.StockMapper;
import com.tonghui.erp.Service.FinishedStockWarningService;
import com.tonghui.erp.Service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成品库存预警服务实现类
 * <p>
 * 取成品出库台账前 N 个完整自然月的已出库数量计算月平均出库量，
 * 取成品合格库存计算可用库存（可按需剔除近效期批次），
 * 再按“可用库存 ÷ 月平均出库量”计算可支撑时间并生成预警级别。
 * 所有品种在内存中合并、排序、分页，避免逐品种查询数据库。
 * </p>
 */
@Service
public class FinishedStockWarningServiceImpl implements FinishedStockWarningService {

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /** 月份格式化器（yyyy-MM） */
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /** 零值 */
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /** 无月均出库量时用于排序的兜底可支撑月数 */
    private static final BigDecimal NO_SUPPORT_MONTHS = new BigDecimal("999999");

    /** 预警级别：不足1个月 */
    private static final String LEVEL_DANGER = "danger";

    /** 预警级别：低于预警阈值 */
    private static final String LEVEL_WARNING = "warning";

    /** 预警级别：正常 */
    private static final String LEVEL_NORMAL = "normal";

    /** 预警级别：无出库 */
    private static final String LEVEL_NO_DEMAND = "noDemand";

    // endregion

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 制剂数据访问层 */
    @Autowired
    private PreparationMapper preparationMapper;

    /** 库存数据访问层 */
    @Autowired
    private StockMapper stockMapper;

    /** 成品出库台账数据访问层 */
    @Autowired
    private SalesOrderMapper salesOrderMapper;

    /** 系统配置服务 */
    @Autowired
    private SystemConfigService systemConfigService;

    // endregion

    // region 对外查询方法
    // ===================================
    // 对外查询方法
    // ===================================

    /**
     * 分页查询成品库存预警列表
     *
     * @param keyword           品种编码或名称关键字（可选，模糊匹配）
     * @param onlyWarning       是否只返回触发预警的品种
     * @param excludeNearExpiry 是否剔除近效期库存
     * @param pageIndex         页码（从0开始）
     * @param pageSize          每页大小
     * @return 分页后的预警列表
     */
    @Override
    public PagedResult<FinishedStockWarningDto> queryWarnings(String keyword,
                                                              boolean onlyWarning,
                                                              boolean excludeNearExpiry,
                                                              int pageIndex,
                                                              int pageSize) {
        WarningResult result = loadWarningResult(excludeNearExpiry);
        List<FinishedStockWarningDto> filtered = filterRows(result.rows, keyword, onlyWarning);

        int safePageIndex = Math.max(0, pageIndex);
        int safePageSize = pageSize <= 0 ? 20 : pageSize;
        int total = filtered.size();
        long fromLong = (long) safePageIndex * safePageSize;
        int from = (int) Math.min(fromLong, total);
        int to = Math.min(from + safePageSize, total);

        PagedResult<FinishedStockWarningDto> pagedResult = new PagedResult<>();
        pagedResult.setItems(new ArrayList<>(filtered.subList(from, to)));
        pagedResult.setTotalCount(total);
        pagedResult.setPageIndex(safePageIndex);
        pagedResult.setPageSize(safePageSize);
        return pagedResult;
    }

    /**
     * 获取成品库存预警汇总数据
     *
     * @param excludeNearExpiry 是否剔除近效期库存
     * @return 预警数量统计与当前统计口径
     */
    @Override
    public FinishedStockWarningSummaryDto getSummary(boolean excludeNearExpiry) {
        WarningResult result = loadWarningResult(excludeNearExpiry);

        FinishedStockWarningSummaryDto summary = new FinishedStockWarningSummaryDto();
        summary.setTotalProducts(result.rows.size());
        summary.setWarningCount(result.rows.stream().filter(FinishedStockWarningDto::isWarning).count());
        summary.setDangerCount(result.rows.stream()
                .filter(row -> LEVEL_DANGER.equals(row.getWarningLevel())).count());
        summary.setNormalCount(result.rows.stream()
                .filter(row -> LEVEL_NORMAL.equals(row.getWarningLevel())).count());
        summary.setNoDemandCount(result.rows.stream()
                .filter(row -> LEVEL_NO_DEMAND.equals(row.getWarningLevel())).count());
        summary.setNoStockCount(result.rows.stream()
                .filter(row -> row.getAvailableStock() != null
                        && row.getAvailableStock().compareTo(ZERO) <= 0)
                .count());
        summary.setAvgMonths(result.avgMonths);
        summary.setWarningMonths(result.warningMonths);
        summary.setNearExpiryDays(result.nearExpiryDays);
        summary.setAvgStartMonth(result.avgStartMonth);
        summary.setAvgEndMonth(result.avgEndMonth);
        summary.setExcludeNearExpiry(excludeNearExpiry);
        return summary;
    }

    /**
     * 获取全部触发预警的品种（用于首页待办）
     *
     * @param excludeNearExpiry 是否剔除近效期库存
     * @return 触发预警的品种列表，按紧急程度排序
     */
    @Override
    public List<FinishedStockWarningDto> getWarnings(boolean excludeNearExpiry) {
        WarningResult result = loadWarningResult(excludeNearExpiry);
        return result.rows.stream()
                .filter(FinishedStockWarningDto::isWarning)
                .collect(Collectors.toList());
    }

    // endregion

    // region 预警计算
    // ===================================
    // 预警计算
    // ===================================

    /**
     * 加载完整预警计算结果
     * <p>
     * 一次性查询启用制剂、成品库存聚合、出库月度聚合，按品种编码合并后计算预警行。
     * 统计窗口为前 N 个完整自然月（不含当月），避免当月未结束导致月均值偏低。
     * </p>
     *
     * @param excludeNearExpiry 是否剔除近效期库存
     * @return 计算结果（含统计口径与全部品种行）
     */
    private WarningResult loadWarningResult(boolean excludeNearExpiry) {
        int avgMonths = positiveInt(systemConfigService.getInt(
                SystemConfigService.KEY_FINISHED_STOCK_AVG_MONTHS,
                SystemConfigService.DEFAULT_FINISHED_STOCK_AVG_MONTHS),
                SystemConfigService.DEFAULT_FINISHED_STOCK_AVG_MONTHS);
        int warningMonths = positiveInt(systemConfigService.getInt(
                SystemConfigService.KEY_FINISHED_STOCK_WARNING_MONTHS,
                SystemConfigService.DEFAULT_FINISHED_STOCK_WARNING_MONTHS),
                SystemConfigService.DEFAULT_FINISHED_STOCK_WARNING_MONTHS);
        int nearExpiryDays = nonNegativeInt(systemConfigService.getInt(
                SystemConfigService.KEY_FINISHED_STOCK_NEAR_EXPIRY_DAYS,
                SystemConfigService.DEFAULT_FINISHED_STOCK_NEAR_EXPIRY_DAYS),
                SystemConfigService.DEFAULT_FINISHED_STOCK_NEAR_EXPIRY_DAYS);

        LocalDate today = LocalDate.now();
        // 统计窗口：前 N 个完整自然月，结束于当月1日（不含当月）
        LocalDate endExclusive = today.withDayOfMonth(1);
        LocalDate start = endExclusive.minusMonths(avgMonths);

        // 1. 启用制剂（主数据）
        List<Preparation> preparations = preparationMapper.selectList(
                new QueryWrapper<Preparation>()
                        .eq("is_deleted", 0)
                        .eq("status", 1)
                        .select("preparation_id", "preparation_code", "preparation_name", "spec", "unit_name"));

        // 2. 成品库存聚合（含近效期、已过期数量）
        List<FinishedStockAggDto> stockAggs =
                stockMapper.selectFinishedStockAgg(today.plusDays(nearExpiryDays));

        // 3. 成品已出库按月聚合
        List<FinishedOutboundMonthlyDto> outboundMonthlyList =
                salesOrderMapper.selectFinishedOutboundMonthly(
                        start.atStartOfDay(), endExclusive.atStartOfDay());

        // 4. 按品种编码合并
        Map<String, WarningRow> rowMap = new LinkedHashMap<>();
        for (Preparation preparation : preparations) {
            String code = normalizeCode(preparation.getPreparationCode());
            if (!StringUtils.hasText(code)) {
                continue;
            }
            WarningRow row = rowMap.computeIfAbsent(code, WarningRow::new);
            row.preparationId = preparation.getPreparationId();
            if (StringUtils.hasText(preparation.getPreparationName())) {
                row.preparationName = preparation.getPreparationName();
            }
            row.spec = preparation.getSpec();
            if (StringUtils.hasText(preparation.getUnitName())) {
                row.unitName = preparation.getUnitName();
            }
        }
        for (FinishedStockAggDto stockAgg : stockAggs) {
            String code = normalizeCode(stockAgg.getPreparationCode());
            if (!StringUtils.hasText(code)) {
                continue;
            }
            WarningRow row = rowMap.computeIfAbsent(code, WarningRow::new);
            if (!StringUtils.hasText(row.preparationName)) {
                row.preparationName = stockAgg.getPreparationName();
            }
            if (!StringUtils.hasText(row.unitName)) {
                row.unitName = stockAgg.getUnitName();
            }
            row.totalStock = nvl(stockAgg.getTotalStock());
            row.qualifiedStock = nvl(stockAgg.getQualifiedStock());
            row.nearExpiryStock = nvl(stockAgg.getNearExpiryStock());
            row.expiredStock = nvl(stockAgg.getExpiredStock());
        }
        for (FinishedOutboundMonthlyDto outbound : outboundMonthlyList) {
            String code = normalizeCode(outbound.getPreparationCode());
            if (!StringUtils.hasText(code)) {
                continue;
            }
            WarningRow row = rowMap.computeIfAbsent(code, WarningRow::new);
            if (!StringUtils.hasText(row.preparationName)) {
                row.preparationName = outbound.getPreparationName();
            }
            BigDecimal quantity = nvl(outbound.getQuantity());
            row.totalOutbound = row.totalOutbound.add(quantity);
            if (StringUtils.hasText(outbound.getMonth())) {
                row.monthlyOutbound.merge(outbound.getMonth(), quantity, BigDecimal::add);
            }
        }

        // 5. 计算每个品种的月均、可用库存、可支撑时间与预警级别
        List<FinishedStockWarningDto> rows = rowMap.values().stream()
                .map(row -> toWarningDto(row, avgMonths, warningMonths,
                        excludeNearExpiry, start, endExclusive))
                .collect(Collectors.toList());
        sortRows(rows);

        WarningResult result = new WarningResult();
        result.avgMonths = avgMonths;
        result.warningMonths = warningMonths;
        result.nearExpiryDays = nearExpiryDays;
        result.avgStartMonth = start.format(MONTH_FORMATTER);
        result.avgEndMonth = endExclusive.minusMonths(1).format(MONTH_FORMATTER);
        result.rows = rows;
        return result;
    }

    /**
     * 将品种合并行转换为前端展示 DTO
     *
     * @param row               品种合并行
     * @param avgMonths         统计月数
     * @param warningMonths     预警阈值（月）
     * @param excludeNearExpiry 是否剔除近效期库存
     * @param start             统计开始月份（含）
     * @param endExclusive      统计结束月份（不含）
     * @return 预警行 DTO
     */
    private FinishedStockWarningDto toWarningDto(WarningRow row,
                                                  int avgMonths,
                                                  int warningMonths,
                                                  boolean excludeNearExpiry,
                                                  LocalDate start,
                                                  LocalDate endExclusive) {
        BigDecimal totalStock = nvl(row.totalStock);
        BigDecimal qualifiedStock = nvl(row.qualifiedStock);
        BigDecimal nearExpiryStock = nvl(row.nearExpiryStock);
        BigDecimal availableStock = excludeNearExpiry
                ? qualifiedStock.subtract(nearExpiryStock) : qualifiedStock;
        if (availableStock.signum() < 0) {
            availableStock = ZERO;
        }

        BigDecimal totalOutbound = nvl(row.totalOutbound);
        BigDecimal autoMonthlyAvg = totalOutbound.divide(
                BigDecimal.valueOf(avgMonths), 2, RoundingMode.HALF_UP);

        BigDecimal supportMonths = null;
        if (autoMonthlyAvg.signum() > 0) {
            supportMonths = availableStock.divide(autoMonthlyAvg, 2, RoundingMode.HALF_UP);
        }

        boolean warning = autoMonthlyAvg.signum() > 0
                && supportMonths != null
                && supportMonths.compareTo(BigDecimal.valueOf(warningMonths)) < 0;
        String warningLevel;
        if (autoMonthlyAvg.signum() <= 0) {
            warningLevel = LEVEL_NO_DEMAND;
        } else if (supportMonths.compareTo(BigDecimal.ONE) < 0) {
            warningLevel = LEVEL_DANGER;
        } else if (warning) {
            warningLevel = LEVEL_WARNING;
        } else {
            warningLevel = LEVEL_NORMAL;
        }

        FinishedStockWarningDto dto = new FinishedStockWarningDto();
        dto.setPreparationId(row.preparationId);
        dto.setPreparationCode(row.preparationCode);
        dto.setPreparationName(StringUtils.hasText(row.preparationName)
                ? row.preparationName : row.preparationCode);
        dto.setSpec(row.spec);
        dto.setUnitName(row.unitName);
        dto.setTotalStock(totalStock);
        dto.setQualifiedStock(qualifiedStock);
        dto.setAvailableStock(availableStock);
        dto.setNearExpiryStock(nearExpiryStock);
        dto.setExpiredStock(nvl(row.expiredStock));
        dto.setTotalOutbound(totalOutbound);
        dto.setAutoMonthlyAvg(autoMonthlyAvg);
        dto.setSupportMonths(supportMonths);
        dto.setWarningMonths(warningMonths);
        dto.setWarning(warning);
        dto.setWarningLevel(warningLevel);
        dto.setAvgMonths(avgMonths);
        dto.setAvgStartMonth(start.format(MONTH_FORMATTER));
        dto.setAvgEndMonth(endExclusive.minusMonths(1).format(MONTH_FORMATTER));
        dto.setOutboundMonths(buildMonthlyOutbound(row, start, endExclusive));
        return dto;
    }

    /**
     * 构建统计窗口内各月出库明细，无出库月份补 0
     *
     * @param row          品种合并行
     * @param start        统计开始月份（含）
     * @param endExclusive 统计结束月份（不含）
     * @return 月度出库明细列表
     */
    private List<MonthlyOutboundDto> buildMonthlyOutbound(WarningRow row,
                                                          LocalDate start,
                                                          LocalDate endExclusive) {
        List<MonthlyOutboundDto> months = new ArrayList<>();
        LocalDate cursor = start;
        while (cursor.isBefore(endExclusive)) {
            String month = cursor.format(MONTH_FORMATTER);
            MonthlyOutboundDto item = new MonthlyOutboundDto();
            item.setMonth(month);
            item.setQuantity(row.monthlyOutbound.getOrDefault(month, ZERO));
            months.add(item);
            cursor = cursor.plusMonths(1);
        }
        return months;
    }

    // endregion

    // region 过滤与排序
    // ===================================
    // 过滤与排序
    // ===================================

    /**
     * 按关键字与预警状态过滤
     *
     * @param rows        全部品种行
     * @param keyword     品种编码/名称关键字（可选）
     * @param onlyWarning 是否只保留预警品种
     * @return 过滤后的行
     */
    private List<FinishedStockWarningDto> filterRows(List<FinishedStockWarningDto> rows,
                                                     String keyword,
                                                     boolean onlyWarning) {
        String normalizedKeyword = StringUtils.hasText(keyword)
                ? keyword.trim().toLowerCase() : null;
        return rows.stream()
                .filter(row -> !onlyWarning || row.isWarning())
                .filter(row -> {
                    if (normalizedKeyword == null) {
                        return true;
                    }
                    String code = row.getPreparationCode() == null
                            ? "" : row.getPreparationCode().toLowerCase();
                    String name = row.getPreparationName() == null
                            ? "" : row.getPreparationName().toLowerCase();
                    return code.contains(normalizedKeyword) || name.contains(normalizedKeyword);
                })
                .collect(Collectors.toList());
    }

    /**
     * 按紧急程度排序：危险 → 预警 → 正常 → 无出库，同级别按可支撑时间升序
     *
     * @param rows 待排序的行
     */
    private void sortRows(List<FinishedStockWarningDto> rows) {
        rows.sort(Comparator
                .comparingInt((FinishedStockWarningDto row) -> warningRank(row.getWarningLevel()))
                .thenComparing(row -> row.getSupportMonths() == null
                        ? NO_SUPPORT_MONTHS : row.getSupportMonths())
                .thenComparing(row -> row.getPreparationCode() == null
                        ? "" : row.getPreparationCode()));
    }

    /**
     * 预警级别排序权重
     *
     * @param warningLevel 预警级别
     * @return 权重（越小越靠前）
     */
    private int warningRank(String warningLevel) {
        if (LEVEL_DANGER.equals(warningLevel)) {
            return 0;
        }
        if (LEVEL_WARNING.equals(warningLevel)) {
            return 1;
        }
        if (LEVEL_NORMAL.equals(warningLevel)) {
            return 2;
        }
        return 3;
    }

    // endregion

    // region 内部工具方法
    // ===================================
    // 内部工具方法
    // ===================================

    /**
     * 空值转零
     *
     * @param value 原值
     * @return 非空 BigDecimal
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    /**
     * 去除品种编码首尾空白
     *
     * @param code 原始编码
     * @return 去空白后的编码
     */
    private String normalizeCode(String code) {
        return code == null ? null : code.trim();
    }

    /**
     * 正整数兜底
     *
     * @param value        原值
     * @param defaultValue 默认值
     * @return 大于0时返回原值，否则返回默认值
     */
    private int positiveInt(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    /**
     * 非负整数兜底
     *
     * @param value        原值
     * @param defaultValue 默认值
     * @return 大于等于0时返回原值，否则返回默认值
     */
    private int nonNegativeInt(int value, int defaultValue) {
        return value >= 0 ? value : defaultValue;
    }

    // endregion

    // region 内部数据结构
    // ===================================
    // 内部数据结构
    // ===================================

    /**
     * 预警计算结果（统计口径 + 品种行）
     */
    private static class WarningResult {

        /** 统计月数 */
        private int avgMonths;

        /** 预警阈值（月） */
        private int warningMonths;

        /** 近效期天数 */
        private int nearExpiryDays;

        /** 统计窗口起始月份 */
        private String avgStartMonth;

        /** 统计窗口结束月份 */
        private String avgEndMonth;

        /** 全部品种行 */
        private List<FinishedStockWarningDto> rows;
    }

    /**
     * 品种合并行（库存、出库数据按品种编码累加）
     */
    private static class WarningRow {

        /** 制剂编码 */
        private final String preparationCode;

        /** 制剂ID */
        private Long preparationId;

        /** 制剂名称 */
        private String preparationName;

        /** 规格 */
        private String spec;

        /** 计量单位 */
        private String unitName;

        /** 成品总库存（含全部状态） */
        private BigDecimal totalStock = ZERO;

        /** 合格库存 */
        private BigDecimal qualifiedStock = ZERO;

        /** 近效期库存 */
        private BigDecimal nearExpiryStock = ZERO;

        /** 已过期库存 */
        private BigDecimal expiredStock = ZERO;

        /** 统计窗口内出库合计 */
        private BigDecimal totalOutbound = ZERO;

        /** 月份 -> 出库数量 */
        private final Map<String, BigDecimal> monthlyOutbound = new LinkedHashMap<>();

        /**
         * 构造品种合并行
         *
         * @param preparationCode 制剂编码
         */
        private WarningRow(String preparationCode) {
            this.preparationCode = preparationCode;
        }
    }

    // endregion
}
