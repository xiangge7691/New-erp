package com.tonghui.erp.Common.utils;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * 日期时间解析工具类
 * <p>
 * 提供统一的日期时间字符串解析方法，兼容以下两种格式：
 * <ul>
 *   <li>空格格式：yyyy-MM-dd HH:mm:ss（含毫秒变体 yyyy-MM-dd HH:mm:ss.SSS）</li>
 *   <li>ISO格式：yyyy-MM-dd'T'HH:mm:ss（含毫秒变体，Jackson默认格式）</li>
 * </ul>
 * 纯日期格式：yyyy-MM-dd
 * </p>
 */
public class DateTimeUtils {

    // region 常量定义
    // ===================================
    // 常量定义
    // ===================================

    /** 空格格式日期时间格式器（yyyy-MM-dd HH:mm:ss） */
    private static final DateTimeFormatter SPACE_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 空格格式日期时间格式器（含毫秒 yyyy-MM-dd HH:mm:ss.SSS） */
    private static final DateTimeFormatter SPACE_DATE_TIME_MILLIS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // endregion

    // region 日期时间解析
    // ===================================
    // 日期时间解析
    // ===================================

    /**
     * 解析日期时间字符串（兼容空格与ISO两种格式）
     * <p>
     * 支持格式：yyyy-MM-dd HH:mm:ss、yyyy-MM-dd HH:mm:ss.SSS、
     * yyyy-MM-dd'T'HH:mm:ss、yyyy-MM-dd'T'HH:mm:ss.SSS
     * </p>
     *
     * @param text 日期时间字符串（可带首尾空格）
     * @return 解析后的 LocalDateTime
     * @throws DateTimeParseException 文本为空或所有格式均解析失败时抛出
     */
    public static LocalDateTime parseDateTime(String text) {
        String trimmed = trimText(text);
        if (trimmed == null) {
            throw new DateTimeParseException("日期时间字符串为空", text == null ? "" : text, 0);
        }
        // 尝试ISO格式（含T分隔符及毫秒）
        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ignored) {
        }
        // 尝试空格格式 yyyy-MM-dd HH:mm:ss
        try {
            return LocalDateTime.parse(trimmed, SPACE_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        // 尝试空格格式含毫秒 yyyy-MM-dd HH:mm:ss.SSS
        try {
            return LocalDateTime.parse(trimmed, SPACE_DATE_TIME_MILLIS_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        throw new DateTimeParseException("无法解析的日期时间格式: " + trimmed, trimmed, 0);
    }

    /**
     * 安全解析日期时间字符串（兼容空格与ISO两种格式）
     *
     * @param text 日期时间字符串（可带首尾空格）
     * @return 解析后的 LocalDateTime，解析失败或文本为空返回 null
     */
    public static LocalDateTime tryParseDateTime(String text) {
        try {
            return parseDateTime(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 解析日期字符串（ISO格式 yyyy-MM-dd）
     *
     * @param text 日期字符串（可带首尾空格）
     * @return 解析后的 LocalDate
     * @throws DateTimeParseException 文本为空或解析失败时抛出
     */
    public static LocalDate parseDate(String text) {
        String trimmed = trimText(text);
        if (trimmed == null) {
            throw new DateTimeParseException("日期字符串为空", text == null ? "" : text, 0);
        }
        return LocalDate.parse(trimmed);
    }

    /**
     * 安全解析日期字符串（ISO格式 yyyy-MM-dd）
     *
     * @param text 日期字符串（可带首尾空格）
     * @return 解析后的 LocalDate，解析失败或文本为空返回 null
     */
    public static LocalDate tryParseDate(String text) {
        try {
            return parseDate(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 解析日期时间字符串并转换为 java.util.Date（兼容空格与ISO两种格式）
     *
     * @param text 日期时间字符串（可带首尾空格）
     * @return 转换后的 Date，解析失败或文本为空返回 null
     */
    public static Date parseToDate(String text) {
        LocalDateTime dateTime = tryParseDateTime(text);
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    // endregion

    // region 私有工具方法
    // ===================================
    // 私有工具方法
    // ===================================

    /**
     * 去除文本首尾空格
     *
     * @param text 原始文本（可为null）
     * @return 去除空格后的文本，空文本返回 null
     */
    private static String trimText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    // endregion
}