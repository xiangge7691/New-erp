package com.tonghui.erp.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonghui.erp.Common.utils.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日期时间格式兼容性测试
 * <p>
 * 验证空格格式（yyyy-MM-dd HH:mm:ss）与ISO格式（yyyy-MM-dd'T'HH:mm:ss）
 * 两种时间格式在工具解析与Jackson JSON反序列化中均可用
 * </p>
 */
@SpringBootTest
public class DateTimeFormatTest {

    // region 依赖注入
    // ===================================
    // 依赖注入
    // ===================================

    /** Spring管理的ObjectMapper（验证请求体JSON反序列化） */
    @Autowired
    private ObjectMapper objectMapper;

    // endregion

    // region 测试方法
    // ===================================
    // 测试方法
    // ===================================

    /**
     * 测试工具类解析两种时间格式
     */
    @Test
    public void testDateTimeUtilsParse() {
        // 空格格式
        LocalDateTime space = DateTimeUtils.parseDateTime("2026-08-10 10:00:00");
        if (space.getHour() != 10) {
            System.err.println("测试失败: 空格格式解析错误: " + space);
        } else {
            System.out.println("空格格式解析成功: " + space);
        }
        // ISO格式
        LocalDateTime iso = DateTimeUtils.parseDateTime("2026-08-10T10:00:00");
        if (iso.getHour() != 10) {
            System.err.println("测试失败: ISO格式解析错误: " + iso);
        } else {
            System.out.println("ISO格式解析成功: " + iso);
        }
        // 空格格式含毫秒
        LocalDateTime spaceMillis = DateTimeUtils.parseDateTime("2026-08-10 10:00:00.123");
        if (spaceMillis.getNano() != 123000000) {
            System.err.println("测试失败: 空格格式含毫秒解析错误: " + spaceMillis);
        } else {
            System.out.println("空格格式含毫秒解析成功: " + spaceMillis);
        }
        // ISO格式含毫秒
        LocalDateTime isoMillis = DateTimeUtils.parseDateTime("2026-08-10T10:00:00.123");
        if (isoMillis.getNano() != 123000000) {
            System.err.println("测试失败: ISO格式含毫秒解析错误: " + isoMillis);
        } else {
            System.out.println("ISO格式含毫秒解析成功: " + isoMillis);
        }
        // 纯日期格式
        LocalDate date = DateTimeUtils.parseDate("2026-08-10");
        if (date.getDayOfMonth() != 10) {
            System.err.println("测试失败: 日期解析错误: " + date);
        } else {
            System.out.println("日期解析成功: " + date);
        }
        // 非法格式返回null
        if (DateTimeUtils.tryParseDateTime("2026-08-10") != null) {
            System.err.println("测试失败: 纯日期不应解析为LocalDateTime");
        } else {
            System.out.println("非法格式安全返回null");
        }
        // 两种格式解析结果一致
        if (!space.equals(iso)) {
            System.err.println("测试失败: 两种格式解析结果不一致");
        } else {
            System.out.println("两种格式解析结果一致");
        }
    }

    /**
     * 测试Jackson JSON反序列化兼容两种时间格式
     */
    @Test
    public void testJacksonDeserialize() {
        try {
            // 空格格式
            TimeDto spaceDto = objectMapper.readValue(
                    "{\"time\":\"2026-08-10 10:00:00\",\"date\":\"2026-08-10\"}", TimeDto.class);
            if (spaceDto.getTime().getHour() != 10) {
                System.err.println("测试失败: JSON空格格式反序列化错误");
            } else {
                System.out.println("JSON空格格式反序列化成功: " + spaceDto.getTime());
            }
            // ISO格式
            TimeDto isoDto = objectMapper.readValue(
                    "{\"time\":\"2026-08-10T10:00:00\",\"date\":\"2026-08-10\"}", TimeDto.class);
            if (isoDto.getTime().getHour() != 10) {
                System.err.println("测试失败: JSON ISO格式反序列化错误");
            } else {
                System.out.println("JSON ISO格式反序列化成功: " + isoDto.getTime());
            }
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // endregion

    // region 测试辅助类
    // ===================================
    // 测试辅助类
    // ===================================

    /**
     * 包含时间字段的测试DTO
     */
    public static class TimeDto {
        /** 日期时间字段 */
        private LocalDateTime time;
        /** 日期字段 */
        private LocalDate date;

        public LocalDateTime getTime() {
            return time;
        }

        public void setTime(LocalDateTime time) {
            this.time = time;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }
    }

    // endregion
}