package com.tonghui.erp.Common.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 客户端 MAC 地址工具类单元测试
 * <p>
 * 覆盖常见 MAC 格式规范化、非法值过滤、查询片段处理与请求头采集，不依赖数据库
 * </p>
 */
public class ClientMacUtilsTest {

    // region 规范化
    // ===================================
    // 规范化
    // ===================================

    /**
     * 常见 MAC 格式统一为短横线大写格式
     */
    @Test
    public void testNormalizeCommonFormats() {
        assertEquals("3C-52-82-1A-4F-09", ClientMacUtils.normalize("3C-52-82-1A-4F-09"));
        assertEquals("3C-52-82-1A-4F-09", ClientMacUtils.normalize("3c:52:82:1a:4f:09"));
        assertEquals("3C-52-82-1A-4F-09", ClientMacUtils.normalize("3C52.821A.4F09"));
        assertEquals("3C-52-82-1A-4F-09", ClientMacUtils.normalize("3c52821a4f09"));
        assertEquals("3C-52-82-1A-4F-09", ClientMacUtils.normalize("  3C-52-82-1A-4F-09  "));
    }

    /**
     * 非法 MAC 返回 null
     */
    @Test
    public void testNormalizeRejectsInvalid() {
        assertNull(ClientMacUtils.normalize(null));
        assertNull(ClientMacUtils.normalize(""));
        assertNull(ClientMacUtils.normalize("3C-52-82-1A-4F"));
        assertNull(ClientMacUtils.normalize("GG-52-82-1A-4F-09"));
        assertNull(ClientMacUtils.normalize("MAC=3C-52-82-1A-4F-09"));
        assertNull(ClientMacUtils.normalize("3C-52-82-1A-4F-09-AA"));
    }

    /**
     * 查询关键字支持完整地址与后缀片段
     */
    @Test
    public void testNormalizeQuery() {
        assertEquals("3C52821A4F09", ClientMacUtils.normalizeQuery("3C-52-82-1A-4F-09"));
        assertEquals("3C52821A4F09", ClientMacUtils.normalizeQuery("3c:52:82:1a:4f:09"));
        assertEquals("4F09", ClientMacUtils.normalizeQuery("4F09"));
        assertEquals("4F09", ClientMacUtils.normalizeQuery("4f-09"));
        assertEquals("3C52", ClientMacUtils.normalizeQuery("3C 52"));
        assertNull(ClientMacUtils.normalizeQuery(null));
        assertNull(ClientMacUtils.normalizeQuery("hello"));
        assertNull(ClientMacUtils.normalizeQuery("3C52821A4F09AA"));
    }

    // endregion

    // region 请求头采集
    // ===================================
    // 请求头采集
    // ===================================

    /**
     * 优先读取 X-Client-Mac，并兼容其他头名
     */
    @Test
    public void testResolveHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Client-Mac", "3c:52:82:1a:4f:09");
        assertEquals("3C-52-82-1A-4F-09", ClientMacUtils.resolve(request));

        MockHttpServletRequest fallback = new MockHttpServletRequest();
        fallback.addHeader("X-Mac-Address", "3C52.821A.4F09");
        assertEquals("3C-52-82-1A-4F-09", ClientMacUtils.resolve(fallback));

        MockHttpServletRequest invalidPrimary = new MockHttpServletRequest();
        invalidPrimary.addHeader("X-Client-Mac", "invalid");
        invalidPrimary.addHeader("Mac-Address", "3C-52-82-1A-4F-09");
        assertEquals("3C-52-82-1A-4F-09", ClientMacUtils.resolve(invalidPrimary));

        assertNull(ClientMacUtils.resolve(null));
        assertNull(ClientMacUtils.resolve(new MockHttpServletRequest()));
    }

    // endregion
}
