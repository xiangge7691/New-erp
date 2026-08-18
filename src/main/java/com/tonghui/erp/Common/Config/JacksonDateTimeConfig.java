package com.tonghui.erp.Common.Config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.tonghui.erp.Common.utils.DateTimeUtils;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Jackson日期时间反序列化配置
 * <p>
 * 全局配置 LocalDateTime/LocalDate 的反序列化器，兼容两种时间格式：
 * <ul>
 *   <li>空格格式：2026-08-10 10:00:00</li>
 *   <li>ISO格式：2026-08-10T10:00:00</li>
 * </ul>
 * 适用于请求体JSON中的时间字段，序列化输出保持默认ISO格式
 * </p>
 */
@Configuration
public class JacksonDateTimeConfig {

    // region Bean定义
    // ===================================
    // Bean定义
    // ===================================

    /**
     * 注册灵活日期时间反序列化器
     *
     * @return Jackson定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer flexibleDateTimeCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
            module.addDeserializer(LocalDate.class, new FlexibleLocalDateDeserializer());
            builder.modulesToInstall(module);
        };
    }

    // endregion

    // region 自定义反序列化器
    // ===================================
    // 自定义反序列化器
    // ===================================

    /**
     * 灵活LocalDateTime反序列化器（兼容空格与ISO格式）
     */
    public static class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

        /**
         * 反序列化LocalDateTime
         *
         * @param p     JSON解析器
         * @param ctxt  反序列化上下文
         * @return 解析后的LocalDateTime
         * @throws IOException 文本为空或格式无法解析时抛出
         */
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.hasToken(JsonToken.VALUE_STRING)) {
                String text = p.getText();
                LocalDateTime parsed = DateTimeUtils.tryParseDateTime(text);
                if (parsed != null) {
                    return parsed;
                }
                throw ctxt.weirdStringException(text, LocalDateTime.class, "无法解析的时间格式，支持 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd'T'HH:mm:ss");
            }
            return (LocalDateTime) ctxt.handleUnexpectedToken(LocalDateTime.class, p);
        }
    }

    /**
     * 灵活LocalDate反序列化器（兼容yyyy-MM-dd格式）
     */
    public static class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

        /**
         * 反序列化LocalDate
         *
         * @param p    JSON解析器
         * @param ctxt 反序列化上下文
         * @return 解析后的LocalDate
         * @throws IOException 文本为空或格式无法解析时抛出
         */
        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.hasToken(JsonToken.VALUE_STRING)) {
                String text = p.getText();
                LocalDate parsed = DateTimeUtils.tryParseDate(text);
                if (parsed != null) {
                    return parsed;
                }
                throw ctxt.weirdStringException(text, LocalDate.class, "无法解析的日期格式，支持 yyyy-MM-dd");
            }
            return (LocalDate) ctxt.handleUnexpectedToken(LocalDate.class, p);
        }
    }

    // endregion
}