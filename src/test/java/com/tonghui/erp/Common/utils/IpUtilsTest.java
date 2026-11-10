package com.tonghui.erp.Common.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客户端 IP 工具类单元测试
 * <p>
 * 覆盖 IPv4/IPv6 规范化、内网判断、代理头优先级与不信任代理头场景，不依赖数据库
 * </p>
 */
public class IpUtilsTest {

    // region 规范化
    // ===================================
    // 规范化
    // ===================================

    /**
     * IPv4 带端口时去掉端口
     */
    @Test
    public void testNormalizeIpv4WithPort() {
        assertEquals("192.168.1.10", IpUtils.normalize("192.168.1.10:8080"));
    }

    /**
     * 方括号 IPv6 带端口时去掉方括号与端口
     */
    @Test
    public void testNormalizeBracketedIpv6() {
        assertEquals("2001:db8::1", IpUtils.normalize("[2001:db8::1]:443"));
    }

    /**
     * IPv4 映射的 IPv6 转为 IPv4
     */
    @Test
    public void testNormalizeMappedIpv4() {
        assertEquals("192.168.1.10", IpUtils.normalize("::ffff:192.168.1.10"));
    }

    /**
     * IPv6 zone id 被移除，并统一转小写
     */
    @Test
    public void testNormalizeZoneAndCase() {
        assertEquals("fe80::1", IpUtils.normalize("FE80::1%eth0"));
    }

    // endregion

    // region 校验与内网判断
    // ===================================
    // 校验与内网判断
    // ===================================

    /**
     * 合法 IPv4/IPv6 识别
     */
    @Test
    public void testIsValidIp() {
        assertTrue(IpUtils.isValidIp("10.0.0.1"));
        assertTrue(IpUtils.isValidIp("240e:3b7:3272:d8d0:db09:c067:8d59:539e"));
        assertTrue(IpUtils.isValidIp("::1"));
        assertFalse(IpUtils.isValidIp("example.com"));
        assertFalse(IpUtils.isValidIp("256.1.1.1"));
        assertFalse(IpUtils.isValidIp(null));
    }

    /**
     * IPv4 私有地址与回环地址识别
     */
    @Test
    public void testIsInternalIpv4() {
        assertTrue(IpUtils.isInternalIp("10.1.2.3"));
        assertTrue(IpUtils.isInternalIp("172.16.0.1"));
        assertTrue(IpUtils.isInternalIp("172.31.255.254"));
        assertTrue(IpUtils.isInternalIp("192.168.1.1"));
        assertTrue(IpUtils.isInternalIp("127.0.0.1"));
        assertFalse(IpUtils.isInternalIp("172.32.0.1"));
        assertFalse(IpUtils.isInternalIp("8.8.8.8"));
    }

    /**
     * IPv6 回环、链路本地、唯一本地地址识别
     */
    @Test
    public void testIsInternalIpv6() {
        assertTrue(IpUtils.isInternalIp("::1"));
        assertTrue(IpUtils.isInternalIp("fe80::1"));
        assertTrue(IpUtils.isInternalIp("fc00::1"));
        assertTrue(IpUtils.isInternalIp("fd12:3456::1"));
        assertFalse(IpUtils.isInternalIp("240e:3b7:3272:d8d0:db09:c067:8d59:539e"));
    }

    // endregion

    // region 代理头解析
    // ===================================
    // 代理头解析
    // ===================================

    /**
     * 信任代理头时优先取 X-Forwarded-For 的第一个合法地址
     */
    @Test
    public void testGetClientIpWithTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");

        assertEquals("203.0.113.5", IpUtils.getClientIp(request, true));
    }

    /**
     * 不信任代理头时只使用 remoteAddr
     */
    @Test
    public void testGetClientIpWithoutTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.5");

        assertEquals("10.0.0.1", IpUtils.getClientIp(request, false));
    }

    /**
     * X-Forwarded-For 中的 IPv6 地址可被正确提取
     */
    @Test
    public void testGetClientIpWithIpv6Forwarded() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "[240e:3b7:3272:d8d0:db09:c067:8d59:539e]:443");

        assertEquals("240e:3b7:3272:d8d0:db09:c067:8d59:539e", IpUtils.getClientIp(request, true));
    }

    // endregion
}
