package com.tonghui.erp.Common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * 首页看板计算工具
 * <p>
 * 统一处理金额取整、百分比、环比、月份解析与月份区间展开，
 * 避免各统计服务口径不一致（例如同一指标在不同接口保留不同小数位）。
 * </p>
 */
public final class DashboardCalcUtils {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    /** 金额小数位（元） */
    private static final int MONEY_SCALE = 2;

    /** 百分比小数位（%） */
    private static final int PERCENT_SCALE = 1;

    /** 月份格式（yyyy-MM） */
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /** 月份正则 */
    private static final String MONTH_REGEX = "^\\d{4}-\\d{2}$";

    /** 内部高精度除法保留位数（分摊场景，避免逐行取整造成合计漂移） */
    private static final int HIGH_SCALE = 8;

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 工具类禁止实例化
     */
    private DashboardCalcUtils() {
    }

    // endregion

    // region 数值计算
    // ===================================
    // 数值计算
    // ===================================

    /**
     * 空值转 0
     *
     * @param value 原始值
     * @return 非空 BigDecimal
     */
    public static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 金额保留 2 位小数（四舍五入）
     *
     * @param value 原始值
     * @return 处理后的金额，空值返回 0.00
     */
    public static BigDecimal money(BigDecimal value) {
        return nz(value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 金额求和后保留 2 位小数
     *
     * @param values 金额列表
     * @return 合计金额
     */
    public static BigDecimal sumMoney(Collection<BigDecimal> values) {
        BigDecimal total = BigDecimal.ZERO;
        if (values != null) {
            for (BigDecimal v : values) {
                total = total.add(nz(v));
            }
        }
        return money(total);
    }

    /**
     * 百分比：part / total × 100，保留 1 位小数
     *
     * @param part  分子
     * @param total 分母
     * @return 百分比；分母为 0 时返回 null
     */
    public static BigDecimal percent(BigDecimal part, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return null;
        }
        return nz(part).multiply(BigDecimal.valueOf(100))
            .divide(total, PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 环比：(current − previous) / |previous| × 100，保留 1 位小数
     *
     * @param current  本期值
     * @param previous 上期值
     * @return 环比百分比；上期为 0 或空时返回 null
     */
    public static BigDecimal momRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return null;
        }
        return nz(current).subtract(previous)
            .multiply(BigDecimal.valueOf(100))
            .divide(previous.abs(), PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 安全除法（高精度），用于分摊
     *
     * @param dividend 被除数
     * @param divisor  除数
     * @return 商；除数为 0 时返回 0
     */
    public static BigDecimal divideHigh(BigDecimal dividend, BigDecimal divisor) {
        if (divisor == null || divisor.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return nz(dividend).divide(divisor, HIGH_SCALE, RoundingMode.HALF_UP);
    }

    // endregion

    // region 月份处理
    // ===================================
    // 月份处理
    // ===================================

    /**
     * 判断字符串是否为 yyyy-MM 格式
     *
     * @param month 月份字符串
     * @return 是否合法
     */
    public static boolean isMonth(String month) {
        return month != null && month.matches(MONTH_REGEX);
    }

    /**
     * 时间转月份字符串
     *
     * @param time 时间
     * @return yyyy-MM；时间为空返回 null
     */
    public static String monthOf(LocalDateTime time) {
        return time == null ? null : time.format(MONTH_FORMATTER);
    }

    /**
     * 日期转月份字符串
     *
     * @param date 日期
     * @return yyyy-MM；日期为空返回 null
     */
    public static String monthOf(LocalDate date) {
        return date == null ? null : date.format(MONTH_FORMATTER);
    }

    /**
     * 月份转当月第一天 00:00:00
     *
     * @param month yyyy-MM
     * @return 当月第一天起始时间；非法月份返回 null
     */
    public static LocalDateTime monthStart(String month) {
        if (!isMonth(month)) {
            return null;
        }
        return YearMonth.parse(month).atDay(1).atStartOfDay();
    }

    /**
     * 月份转当月最后一天 23:59:59
     *
     * @param month yyyy-MM
     * @return 当月最后一天结束时间；非法月份返回 null
     */
    public static LocalDateTime monthEnd(String month) {
        if (!isMonth(month)) {
            return null;
        }
        return YearMonth.parse(month).atEndOfMonth().atTime(23, 59, 59);
    }

    /**
     * 判断月份是否落在区间内（闭区间）
     *
     * @param month     待判断月份
     * @param startMonth 起始月份（可空）
     * @param endMonth   结束月份（可空）
     * @return 是否在区间内
     */
    public static boolean inRange(String month, String startMonth, String endMonth) {
        if (month == null) {
            return false;
        }
        if (isMonth(startMonth) && month.compareTo(startMonth) < 0) {
            return false;
        }
        return !isMonth(endMonth) || month.compareTo(endMonth) <= 0;
    }

    /**
     * 展开月份区间（闭区间）
     *
     * @param startMonth 起始月份（可空，空表示不限制）
     * @param endMonth   结束月份（可空，空表示不限制）
     * @return 月份列表；任一端为空时返回空列表
     */
    public static List<String> expandMonths(String startMonth, String endMonth) {
        List<String> months = new ArrayList<>();
        if (!isMonth(startMonth) || !isMonth(endMonth)) {
            return months;
        }
        YearMonth cursor = YearMonth.parse(startMonth);
        YearMonth end = YearMonth.parse(endMonth);
        while (!cursor.isAfter(end)) {
            months.add(cursor.format(MONTH_FORMATTER));
            cursor = cursor.plusMonths(1);
        }
        return months;
    }

    /**
     * 合并月份集合并排序（升序）
     *
     * @param monthCollections 多组月份
     * @return 去重排序后的月份列表
     */
    @SafeVarargs
    public static List<String> mergeMonths(Collection<String>... monthCollections) {
        TreeSet<String> set = new TreeSet<>(Comparator.naturalOrder());
        if (monthCollections != null) {
            for (Collection<String> months : monthCollections) {
                if (months == null) {
                    continue;
                }
                for (String month : months) {
                    if (isMonth(month)) {
                        set.add(month);
                    }
                }
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * 取上一自然月
     *
     * @param month yyyy-MM
     * @return 上一月；非法月份返回 null
     */
    public static String previousMonth(String month) {
        if (!isMonth(month)) {
            return null;
        }
        return YearMonth.parse(month).minusMonths(1).format(MONTH_FORMATTER);
    }

    // endregion
}
