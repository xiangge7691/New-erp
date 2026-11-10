package com.tonghui.erp.Common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 日志参数脱敏工具
 * <p>
 * 将 Controller 方法参数序列化为 JSON 摘要，跳过 Servlet / 文件等不可序列化对象，
 * 对密码、令牌等敏感字段脱敏，并按配置长度截断
 * </p>
 */
@Component
public class LogParamUtils {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    /** 敏感字段关键字（忽略大小写，字段名包含即脱敏） */
    private static final Set<String> SENSITIVE_KEY_PARTS = Set.of(
            "password", "passwd", "token", "authorization",
            "secret", "privatekey", "credential"
    );

    /** 脱敏后的占位值 */
    private static final String MASKED_VALUE = "******";

    /** 最大递归深度，防止异常对象导致栈溢出 */
    private static final int MAX_DEPTH = 6;

    // endregion

    // region 字段与构造
    // ===================================
    // 字段与构造
    // ===================================

    private final ObjectMapper objectMapper;

    /**
     * 构造参数脱敏工具
     *
     * @param objectMapper Jackson 对象映射器（使用项目全局日期配置）
     */
    public LogParamUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // endregion

    // region 对外方法
    // ===================================
    // 对外方法
    // ===================================

    /**
     * 将方法参数序列化为脱敏后的 JSON 摘要
     *
     * @param args       方法参数数组
     * @param maxLength  最大字符长度，小于等于 0 表示不截断
     * @return JSON 摘要；无参数时返回 null
     */
    public String toJsonSafely(Object[] args, int maxLength) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            ArrayNode array = objectMapper.createArrayNode();
            for (Object arg : args) {
                array.add(toNode(arg, 0));
            }
            String json = objectMapper.writeValueAsString(array);
            return truncate(json, maxLength);
        } catch (Exception e) {
            return "参数序列化失败：" + e.getMessage();
        }
    }

    // endregion

    // region 节点转换与脱敏
    // ===================================
    // 节点转换与脱敏
    // ===================================

    /**
     * 将单个参数转换为 JSON 节点
     *
     * @param arg   参数
     * @param depth 当前深度
     * @return JSON 节点
     */
    private JsonNode toNode(Object arg, int depth) {
        if (arg == null) {
            return objectMapper.nullNode();
        }
        if (depth > MAX_DEPTH) {
            return TextNode.valueOf("[层级过深]");
        }
        if (isIgnoredType(arg)) {
            return TextNode.valueOf("[" + arg.getClass().getSimpleName() + "]");
        }
        if (arg instanceof MultipartFile file) {
            return TextNode.valueOf("[文件]" + safeFileName(file));
        }
        if (arg instanceof byte[]) {
            return TextNode.valueOf("[二进制数据]");
        }
        try {
            JsonNode node = objectMapper.valueToTree(arg);
            return mask(node, depth);
        } catch (Exception e) {
            return TextNode.valueOf("[无法序列化]" + arg.getClass().getSimpleName());
        }
    }

    /**
     * 递归脱敏 JSON 节点
     *
     * @param node  JSON 节点
     * @param depth 当前深度
     * @return 脱敏后的节点
     */
    private JsonNode mask(JsonNode node, int depth) {
        if (node == null) {
            return objectMapper.nullNode();
        }
        if (depth > MAX_DEPTH) {
            return TextNode.valueOf("[层级过深]");
        }
        if (node.isObject()) {
            ObjectNode source = (ObjectNode) node;
            ObjectNode target = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = source.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (isSensitiveKey(entry.getKey())) {
                    target.put(entry.getKey(), MASKED_VALUE);
                } else {
                    target.set(entry.getKey(), mask(entry.getValue(), depth + 1));
                }
            }
            return target;
        }
        if (node.isArray()) {
            ArrayNode target = objectMapper.createArrayNode();
            for (JsonNode item : node) {
                target.add(mask(item, depth + 1));
            }
            return target;
        }
        return node;
    }

    // endregion

    // region 辅助方法
    // ===================================
    // 辅助方法
    // ===================================

    /**
     * 判断参数类型是否应当跳过序列化
     *
     * @param arg 参数
     * @return 是否跳过
     */
    private boolean isIgnoredType(Object arg) {
        return arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof InputStream
                || arg instanceof OutputStream
                || arg instanceof Principal;
    }

    /**
     * 判断字段名是否敏感
     *
     * @param fieldName 字段名
     * @return 是否敏感
     */
    private boolean isSensitiveKey(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String lower = fieldName.toLowerCase();
        for (String part : SENSITIVE_KEY_PARTS) {
            if (lower.contains(part)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取上传文件的原始名称
     *
     * @param file 上传文件
     * @return 文件名
     */
    private String safeFileName(MultipartFile file) {
        try {
            return file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 按字符长度截断
     *
     * @param text      文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null || maxLength <= 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...(已截断)";
    }

    // endregion
}
