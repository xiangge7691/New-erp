package com.tonghui.erp.Common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 日志查询时间参数工具类
 * <p>
 * 兼容前端传入“yyyy-MM-dd”或“yyyy-MM-dd HH:mm:ss”两种格式：
 * 只传日期时，起始时间补 00:00:00，截止时间补 23:59:59
 * </p>
 */
public final class LogQueryTimeUtils {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    /** 日期格式 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 日期正则 */
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 工具类禁止实例化
     */
    private LogQueryTimeUtils() {
    }

    // endregion

    // region 解析方法
    // ===================================
    // 解析方法
    // ===================================

    /**
     * 解析查询起始时间
     *
     * @param text 时间文本
     * @return 起始时间；文本为空返回 null
     */
    public static LocalDateTime parseStart(String text) {
        String trimmed = trim(text);
        if (trimmed == null) {
            return null;
        }
        if (isDateOnly(trimmed)) {
            return LocalDate.parse(trimmed, DATE_FORMATTER).atStartOfDay();
        }
        return parseDateTime(trimmed);
    }

    /**
     * 解析查询截止时间
     *
     * @param text 时间文本
     * @return 截止时间；文本为空返回 null
     */
    public static LocalDateTime parseEnd(String text) {
        String trimmed = trim(text);
        if (trimmed == null) {
            return null;
        }
        if (isDateOnly(trimmed)) {
            return LocalDate.parse(trimmed, DATE_FORMATTER).atTime(23, 59, 59);
        }
        return parseDateTime(trimmed);
    }

    /**
     * 解析日期时间文本
     *
     * @param text 文本
     * @return 日期时间
     */
    private static LocalDateTime parseDateTime(String text) {
        LocalDateTime value = DateTimeUtils.tryParseDateTime(text);
        if (value == null) {
            throw new IllegalArgumentException("时间格式不正确，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss：" + text);
        }
        return value;
    }

    /**
     * 判断是否为纯日期格式
     *
     * @param text 文本
     * @return 是否为 yyyy-MM-dd
     */
    private static boolean isDateOnly(String text) {
        return DATE_PATTERN.matcher(text).matches();
    }

    /**
     * 去除首尾空白，空串转 null
     *
     * @param text 文本
     * @return 处理后的文本
     */
    private static String trim(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // endregion
}
