package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Common.Config.LogProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IP 归属地解析服务单元测试
 * <p>
 * 直接加载项目内置的 IPv4/IPv6 离线库，验证内网、公网 IPv4、公网 IPv6 与非法地址场景
 * </p>
 */
public class IpLocationServiceTest {

    // region 测试对象
    // ===================================
    // 测试对象
    // ===================================

    private IpLocationService ipLocationService;

    /**
     * 每个用例前创建服务（加载内置 xdb）
     */
    @BeforeEach
    public void setUp() {
        ipLocationService = new IpLocationService(new LogProperties());
    }

    /**
     * 每个用例后释放查询服务
     */
    @AfterEach
    public void tearDown() {
        if (ipLocationService != null) {
            ipLocationService.destroy();
        }
    }

    // endregion

    // region 解析用例
    // ===================================
    // 解析用例
    // ===================================

    /**
     * IPv4 内网地址直接返回“内网”
     */
    @Test
    public void testResolveInternalIpv4() {
        assertEquals(IpLocationService.INTERNAL, ipLocationService.resolve("192.168.1.10"));
        assertEquals(IpLocationService.INTERNAL, ipLocationService.resolve("127.0.0.1"));
    }

    /**
     * IPv6 内网地址直接返回“内网”
     */
    @Test
    public void testResolveInternalIpv6() {
        assertEquals(IpLocationService.INTERNAL, ipLocationService.resolve("::1"));
        assertEquals(IpLocationService.INTERNAL, ipLocationService.resolve("fd00::1"));
    }

    /**
     * 公网 IPv4 能解析出非“未知”的归属地
     */
    @Test
    public void testResolvePublicIpv4() {
        String location = ipLocationService.resolve("8.8.8.8");
        assertNotEquals(IpLocationService.UNKNOWN, location);
        assertTrue(location.length() > 0);
    }

    /**
     * 公网 IPv6 能解析出非“未知”的归属地（验证 v6 离线库已加载）
     */
    @Test
    public void testResolvePublicIpv6() {
        String location = ipLocationService.resolve("240e:3b7:3272:d8d0:db09:c067:8d59:539e");
        assertNotEquals(IpLocationService.UNKNOWN, location);
        assertTrue(location.length() > 0);
    }

    /**
     * 非法 IP 返回“未知”
     */
    @Test
    public void testResolveInvalidIp() {
        assertEquals(IpLocationService.UNKNOWN, ipLocationService.resolve("not-an-ip"));
        assertEquals(IpLocationService.UNKNOWN, ipLocationService.resolve(null));
    }

    // endregion
}
