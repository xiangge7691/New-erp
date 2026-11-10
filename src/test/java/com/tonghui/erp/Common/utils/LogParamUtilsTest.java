package com.tonghui.erp.Common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 日志参数脱敏工具单元测试
 * <p>
 * 覆盖敏感字段脱敏、嵌套对象脱敏、不可序列化对象跳过与超长截断
 * </p>
 */
public class LogParamUtilsTest {

    // region 测试对象
    // ===================================
    // 测试对象
    // ===================================

    private final LogParamUtils logParamUtils = new LogParamUtils(new ObjectMapper());

    // endregion

    // region 脱敏用例
    // ===================================
    // 脱敏用例
    // ===================================

    /**
     * 密码、令牌等敏感字段被替换为占位值
     */
    @Test
    public void testMaskSensitiveFields() {
        Map<String, Object> body = new HashMap<>();
        body.put("userAccount", "root");
        body.put("password", "plain-password");
        body.put("refreshToken", "token-value");
        body.put("secretKey", "secret-value");

        String json = logParamUtils.toJsonSafely(new Object[]{body}, 2000);

        assertTrue(json.contains("******"));
        assertFalse(json.contains("plain-password"));
        assertFalse(json.contains("token-value"));
        assertFalse(json.contains("secret-value"));
    }

    /**
     * 嵌套对象中的敏感字段同样被脱敏
     */
    @Test
    public void testMaskNestedSensitiveFields() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("oldPassword", "old-value");
        nested.put("name", "张三");
        Map<String, Object> body = new HashMap<>();
        body.put("payload", nested);

        String json = logParamUtils.toJsonSafely(new Object[]{body}, 2000);

        assertTrue(json.contains("******"));
        assertFalse(json.contains("old-value"));
        assertTrue(json.contains("张三"));
    }

    /**
     * 超长参数按配置长度截断
     */
    @Test
    public void testTruncateLongParams() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            builder.append("abcdefghij");
        }
        String json = logParamUtils.toJsonSafely(new Object[]{builder.toString()}, 50);

        assertTrue(json.endsWith("...(已截断)"));
        assertTrue(json.length() < builder.length());
    }

    /**
     * 无参数时返回 null
     */
    @Test
    public void testEmptyArgs() {
        assertNull(logParamUtils.toJsonSafely(null, 2000));
        assertNull(logParamUtils.toJsonSafely(new Object[]{}, 2000));
    }

    // endregion

    // region 不可序列化对象
    // ===================================
    // 不可序列化对象
    // ===================================

    /**
     * ServletRequest 与上传文件不参与序列化，只保留类型/文件名
     */
    @Test
    public void testSkipServletRequestAndMultipartFile() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        String json = logParamUtils.toJsonSafely(new Object[]{request, file}, 2000);

        assertTrue(json.contains("[MockHttpServletRequest]"));
        assertTrue(json.contains("[文件]test.pdf"));
        assertEquals("[", json.substring(0, 1));
    }

    // endregion
}
