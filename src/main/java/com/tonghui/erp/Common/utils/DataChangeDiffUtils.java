package com.tonghui.erp.Common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonghui.erp.Common.Config.LogProperties;
import com.tonghui.erp.Common.Dto.Log.FieldChange;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据变更差异工具
 * <p>
 * 对比修改前后的整行数据，生成字段差异、完整行快照，并负责敏感字段脱敏、
 * 长值截断与字段中文名（列注释）解析
 * </p>
 */
@Component
public class DataChangeDiffUtils {

    // region 常量与依赖
    // ===================================
    // 常量与依赖
    // ===================================

    private static final Logger log = LoggerFactory.getLogger(DataChangeDiffUtils.class);

    /** 变更类型：新增 */
    public static final String TYPE_INSERT = "INSERT";

    /** 变更类型：修改 */
    public static final String TYPE_UPDATE = "UPDATE";

    /** 变更类型：删除 */
    public static final String TYPE_DELETE = "DELETE";

    /** 不参与差异比较的审计字段 */
    private static final Set<String> AUDIT_FIELDS = Set.of(
            "created_by", "created_time", "updated_by", "updated_time", "version"
    );

    /** 敏感字段关键字 */
    private static final Set<String> SENSITIVE_KEY_PARTS = Set.of(
            "password", "passwd", "token", "authorization",
            "secret", "privatekey", "credential"
    );

    /** 脱敏占位值 */
    private static final String MASKED_VALUE = "******";

    /** 二进制字段占位值 */
    private static final String BINARY_VALUE = "[二进制数据]";

    /** 整行 JSON 最大长度 */
    private static final int MAX_JSON_LENGTH = 60000;

    /** 日期时间格式 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 日期格式 */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 时间格式 */
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final LogProperties logProperties;

    /** 列注释缓存：表名 -> (列名大写 -> 中文注释) */
    private final Map<String, Map<String, String>> columnCommentCache = new ConcurrentHashMap<>();

    /**
     * 构造差异工具
     *
     * @param objectMapper   Jackson 对象映射器
     * @param jdbcTemplate   JDBC模板
     * @param logProperties  日志配置
     */
    public DataChangeDiffUtils(ObjectMapper objectMapper,
                               JdbcTemplate jdbcTemplate,
                               LogProperties logProperties) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.logProperties = logProperties;
    }

    // endregion

    // region 差异计算
    // ===================================
    // 差异计算
    // ===================================

    /**
     * 计算 UPDATE 差异
     *
     * @param tableName 表名
     * @param before    修改前整行
     * @param after     修改后整行
     * @return 差异结果；无业务字段变化且非软删除时返回 null
     */
    public Result diff(String tableName, Map<String, Object> before, Map<String, Object> after) {
        if (before == null && after == null) {
            return null;
        }
        boolean beforeDeleted = isDeleted(before);
        boolean afterDeleted = isDeleted(after);

        List<FieldChange> changes = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        if (before != null) {
            keys.addAll(before.keySet());
        }
        if (after != null) {
            keys.addAll(after.keySet());
        }
        for (String key : keys) {
            if (isAuditField(key)) {
                continue;
            }
            Object beforeValue = before == null ? null : before.get(key);
            Object afterValue = after == null ? null : after.get(key);
            if (sameValue(beforeValue, afterValue)) {
                continue;
            }
            changes.add(new FieldChange(key, columnLabel(tableName, key),
                    fieldValue(key, beforeValue), fieldValue(key, afterValue)));
        }

        String changeType = (!beforeDeleted && afterDeleted) ? TYPE_DELETE : TYPE_UPDATE;
        if (changes.isEmpty() && TYPE_UPDATE.equals(changeType)) {
            return null;
        }
        Result result = new Result();
        result.setChangeType(changeType);
        result.setBeforeJson(toJson(tableName, before));
        result.setAfterJson(toJson(tableName, after));
        result.setChangedFields(changes);
        result.setFieldCount(changes.size());
        return result;
    }

    /**
     * 计算 INSERT 快照
     *
     * @param tableName 表名
     * @param after     新增后的整行
     * @return 差异结果
     */
    public Result insert(String tableName, Map<String, Object> after) {
        if (after == null) {
            return null;
        }
        List<FieldChange> changes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : after.entrySet()) {
            if (isAuditField(entry.getKey()) || "is_deleted".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            if (entry.getValue() == null) {
                continue;
            }
            changes.add(new FieldChange(entry.getKey(), columnLabel(tableName, entry.getKey()),
                    null, fieldValue(entry.getKey(), entry.getValue())));
        }
        Result result = new Result();
        result.setChangeType(TYPE_INSERT);
        result.setAfterJson(toJson(tableName, after));
        result.setChangedFields(changes);
        result.setFieldCount(changes.size());
        return result;
    }

    /**
     * 计算 DELETE 快照
     *
     * @param tableName 表名
     * @param before    删除前的整行
     * @return 差异结果
     */
    public Result delete(String tableName, Map<String, Object> before) {
        if (before == null) {
            return null;
        }
        List<FieldChange> changes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : before.entrySet()) {
            if (isAuditField(entry.getKey()) || "is_deleted".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            if (entry.getValue() == null) {
                continue;
            }
            changes.add(new FieldChange(entry.getKey(), columnLabel(tableName, entry.getKey()),
                    fieldValue(entry.getKey(), entry.getValue()), null));
        }
        Result result = new Result();
        result.setChangeType(TYPE_DELETE);
        result.setBeforeJson(toJson(tableName, before));
        result.setChangedFields(changes);
        result.setFieldCount(changes.size());
        return result;
    }

    // endregion

    // region 值与快照处理
    // ===================================
    // 值与快照处理
    // ===================================

    /**
     * 判断修改前后值是否相同
     *
     * @param beforeValue 修改前值
     * @param afterValue  修改后值
     * @return 是否相同
     */
    private boolean sameValue(Object beforeValue, Object afterValue) {
        if (beforeValue == null && afterValue == null) {
            return true;
        }
        if (beforeValue == null || afterValue == null) {
            return false;
        }
        return canonical(beforeValue).equals(canonical(afterValue));
    }

    /**
     * 生成用于比较的规范字符串
     *
     * @param value 值
     * @return 规范字符串
     */
    private String canonical(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof byte[]) {
            return BINARY_VALUE;
        }
        if (value instanceof Timestamp timestamp) {
            return DATE_TIME_FORMATTER.format(timestamp.toLocalDateTime());
        }
        if (value instanceof LocalDateTime dateTime) {
            return DATE_TIME_FORMATTER.format(dateTime);
        }
        if (value instanceof LocalDate date) {
            return DATE_FORMATTER.format(date);
        }
        if (value instanceof LocalTime time) {
            return TIME_FORMATTER.format(time);
        }
        if (value instanceof java.util.Date date) {
            return DATE_TIME_FORMATTER.format(new Timestamp(date.getTime()).toLocalDateTime());
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        return String.valueOf(value);
    }

    /**
     * 转换为可安全写入 JSON 的值
     *
     * @param value 原始值
     * @return JSON 安全值
     */
    private Object jsonSafe(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return BINARY_VALUE;
        }
        String text;
        if (value instanceof Timestamp timestamp) {
            text = DATE_TIME_FORMATTER.format(timestamp.toLocalDateTime());
        } else if (value instanceof LocalDateTime dateTime) {
            text = DATE_TIME_FORMATTER.format(dateTime);
        } else if (value instanceof LocalDate date) {
            text = DATE_FORMATTER.format(date);
        } else if (value instanceof LocalTime time) {
            text = TIME_FORMATTER.format(time);
        } else if (value instanceof java.util.Date date) {
            text = DATE_TIME_FORMATTER.format(new Timestamp(date.getTime()).toLocalDateTime());
        } else if (value instanceof BigDecimal decimal) {
            text = decimal.toPlainString();
        } else if (value instanceof Number || value instanceof Boolean) {
            return value;
        } else {
            text = String.valueOf(value);
        }
        return truncate(text, maxValueLength());
    }

    /**
     * 转换字段差异中的值，敏感字段统一脱敏
     *
     * @param field 字段名
     * @param value 原始值
     * @return 展示值
     */
    private Object fieldValue(String field, Object value) {
        if (value == null) {
            return null;
        }
        if (isSensitiveField(field)) {
            return MASKED_VALUE;
        }
        return jsonSafe(value);
    }

    /**
     * 生成整行 JSON 快照
     *
     * @param tableName 表名
     * @param row       整行数据
     * @return JSON 字符串；空行返回 null
     */
    private String toJson(String tableName, Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        Map<String, Object> masked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (isAuditField(entry.getKey())) {
                continue;
            }
            String key = entry.getKey();
            if (isSensitiveField(key)) {
                masked.put(key, MASKED_VALUE);
            } else {
                masked.put(key, jsonSafe(entry.getValue()));
            }
        }
        try {
            String json = objectMapper.writeValueAsString(masked);
            if (json.length() > MAX_JSON_LENGTH) {
                Map<String, Object> truncated = new LinkedHashMap<>();
                truncated.put("_truncated", true);
                truncated.put("_preview", json.substring(0, MAX_JSON_LENGTH - 100));
                return objectMapper.writeValueAsString(truncated);
            }
            return json;
        } catch (Exception e) {
            log.debug("数据变更快照序列化失败：表={}，原因={}", tableName, e.getMessage());
            return null;
        }
    }

    // endregion

    // region 字段元数据
    // ===================================
    // 字段元数据
    // ===================================

    /**
     * 获取字段中文名
     *
     * @param tableName 表名
     * @param column    列名
     * @return 中文名；取不到时返回列名
     */
    private String columnLabel(String tableName, String column) {
        if (tableName == null || column == null) {
            return column;
        }
        Map<String, String> comments = columnCommentCache.computeIfAbsent(tableName, this::loadColumnComments);
        String label = comments.get(column.toUpperCase(Locale.ROOT));
        return label == null || label.trim().isEmpty() ? column : label;
    }

    /**
     * 从 information_schema 加载列注释
     *
     * @param tableName 表名
     * @return 列名大写 -> 注释
     */
    private Map<String, String> loadColumnComments(String tableName) {
        Map<String, String> comments = new ConcurrentHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME, COLUMN_COMMENT FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?", tableName);
            for (Map<String, Object> row : rows) {
                Object name = row.get("COLUMN_NAME");
                Object comment = row.get("COLUMN_COMMENT");
                if (name != null && comment != null) {
                    comments.put(String.valueOf(name).toUpperCase(Locale.ROOT), String.valueOf(comment));
                }
            }
        } catch (Exception e) {
            log.debug("加载表字段注释失败：表={}，原因={}", tableName, e.getMessage());
        }
        return comments;
    }

    // endregion

    // region 判断与截断
    // ===================================
    // 判断与截断
    // ===================================

    /**
     * 判断行是否已被逻辑删除
     *
     * @param row 整行数据
     * @return 是否已删除
     */
    public boolean isDeletedRow(Map<String, Object> row) {
        return isDeleted(row);
    }

    /**
     * 判断行是否已被逻辑删除
     *
     * @param row 整行数据
     * @return 是否已删除
     */
    private boolean isDeleted(Map<String, Object> row) {
        if (row == null) {
            return false;
        }
        Object value = row.get("is_deleted");
        if (value == null) {
            value = row.get("IS_DELETED");
        }
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() == 1;
        }
        return "1".equals(String.valueOf(value).trim());
    }

    /**
     * 判断是否审计字段
     *
     * @param field 字段名
     * @return 是否审计字段
     */
    private boolean isAuditField(String field) {
        return field != null && AUDIT_FIELDS.contains(field.toLowerCase(Locale.ROOT));
    }

    /**
     * 判断是否敏感字段
     *
     * @param field 字段名
     * @return 是否敏感
     */
    private boolean isSensitiveField(String field) {
        if (field == null) {
            return false;
        }
        String lower = field.toLowerCase(Locale.ROOT);
        for (String part : SENSITIVE_KEY_PARTS) {
            if (lower.contains(part)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取单字段最大长度
     *
     * @return 最大长度
     */
    private int maxValueLength() {
        int configured = logProperties.getChange().getMaxValueLength();
        return configured > 0 ? configured : 2000;
    }

    /**
     * 按长度截断字符串
     *
     * @param text      文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...(已截断)";
    }

    // endregion

    // region 差异结果
    // ===================================
    // 差异结果
    // ===================================

    /**
     * 差异计算结果
     */
    @Data
    public static class Result {

        /**
         * 变更类型
         */
        private String changeType;

        /**
         * 修改前整行 JSON
         */
        private String beforeJson;

        /**
         * 修改后整行 JSON
         */
        private String afterJson;

        /**
         * 字段差异
         */
        private List<FieldChange> changedFields = new ArrayList<>();

        /**
         * 字段数
         */
        private int fieldCount;
    }

    // endregion
}
