package com.tonghui.erp.Common.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

/**
 * 客户端 IP 工具类
 * <p>
 * 负责从 HTTP 请求中获取真实客户端 IP，并统一处理 IPv4 / IPv6 的常见表示形式：
 * 代理头、端口后缀、方括号、IPv4 映射的 IPv6、IPv6 zone id 等
 * </p>
 */
public final class IpUtils {

    // region 常量与正则
    // ===================================
    // 常量与正则
    // ===================================

    /** 未知 IP 的占位值 */
    private static final String UNKNOWN = "unknown";

    /** IP 字段最大长度（与数据库 VARCHAR(64) 保持一致） */
    private static final int MAX_IP_LENGTH = 60;

    /** IPv4 正则 */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");

    /** IPv4:端口 正则 */
    private static final Pattern IPV4_WITH_PORT_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d):\\d{1,5}$");

    /** IPv6 正则（不含 IPv4 映射形式，映射形式在规范化阶段转成 IPv4） */
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}"
                    + "|([0-9a-fA-F]{1,4}:){1,7}:"
                    + "|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}"
                    + "|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}"
                    + "|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}"
                    + "|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}"
                    + "|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}"
                    + "|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})"
                    + "|:((:[0-9a-fA-F]{1,4}){1,7}|:))$");

    /** IPv4 映射的 IPv6 前缀 */
    private static final String IPV4_MAPPED_PREFIX = "::ffff:";

    /** 代理头，按优先级排列 */
    private static final String[] PROXY_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 工具类禁止实例化
     */
    private IpUtils() {
    }

    // endregion

    // region 获取客户端IP
    // ===================================
    // 获取客户端IP
    // ===================================

    /**
     * 获取真实客户端 IP
     * <p>
     * 仅在确认请求经过自家 Nginx 时才应信任代理头；Nginx 必须覆盖 X-Forwarded-For，
     * 否则客户端可以伪造该头
     * </p>
     *
     * @param request           HTTP请求
     * @param trustProxyHeaders 是否信任代理头
     * @return 规范化后的客户端 IP；无法获取时返回 null
     */
    public static String getClientIp(HttpServletRequest request, boolean trustProxyHeaders) {
        if (request == null) {
            return null;
        }

        if (trustProxyHeaders) {
            for (String header : PROXY_HEADERS) {
                String value = request.getHeader(header);
                if (isBlankOrUnknown(value)) {
                    continue;
                }
                // X-Forwarded-For 可能是 "client, proxy1, proxy2"，取第一个合法地址
                if ("X-Forwarded-For".equalsIgnoreCase(header) || "HTTP_X_FORWARDED_FOR".equalsIgnoreCase(header)) {
                    for (String part : value.split(",")) {
                        String ip = normalize(part);
                        if (isValidIp(ip)) {
                            return truncate(ip);
                        }
                    }
                    continue;
                }
                String ip = normalize(value);
                if (isValidIp(ip)) {
                    return truncate(ip);
                }
            }
        }

        String remoteIp = normalize(request.getRemoteAddr());
        return isBlank(remoteIp) ? null : truncate(remoteIp);
    }

    // endregion

    // region 规范化与校验
    // ===================================
    // 规范化与校验
    // ===================================

    /**
     * 规范化 IP 表示
     * <p>
     * 处理方括号、端口后缀、IPv6 zone id、IPv4 映射的 IPv6，并统一转小写
     * </p>
     *
     * @param rawIp 原始 IP
     * @return 规范化后的 IP；输入为空时返回 null
     */
    public static String normalize(String rawIp) {
        if (isBlank(rawIp)) {
            return null;
        }
        String ip = rawIp.trim();

        // [2001:db8::1] 或 [2001:db8::1]:443
        if (ip.startsWith("[")) {
            int end = ip.indexOf(']');
            if (end > 1) {
                ip = ip.substring(1, end);
            }
        }

        // IPv6 zone id：fe80::1%eth0
        int zoneIndex = ip.indexOf('%');
        if (zoneIndex > 0) {
            ip = ip.substring(0, zoneIndex);
        }

        // IPv4:端口
        if (IPV4_WITH_PORT_PATTERN.matcher(ip).matches()) {
            ip = ip.substring(0, ip.lastIndexOf(':'));
        }

        // IPv4 映射的 IPv6：::ffff:192.168.1.10
        if (ip.toLowerCase().startsWith(IPV4_MAPPED_PREFIX)) {
            String candidate = ip.substring(IPV4_MAPPED_PREFIX.length());
            if (IPV4_PATTERN.matcher(candidate).matches()) {
                ip = candidate;
            }
        }

        if (ip.isEmpty()) {
            return null;
        }
        return ip.toLowerCase();
    }

    /**
     * 校验是否为合法的 IPv4 或 IPv6 地址
     *
     * @param ip 已规范化的 IP
     * @return 是否合法
     */
    public static boolean isValidIp(String ip) {
        if (isBlank(ip)) {
            return false;
        }
        String value = ip.trim().toLowerCase();
        return IPV4_PATTERN.matcher(value).matches() || IPV6_PATTERN.matcher(value).matches();
    }

    /**
     * 判断是否为 IPv6 地址
     *
     * @param ip 已规范化的 IP
     * @return 是否为 IPv6
     */
    public static boolean isIpv6(String ip) {
        return isValidIp(ip) && ip.contains(":");
    }

    /**
     * 判断是否为内网地址
     * <p>
     * 覆盖 IPv4 私有地址、回环地址，以及 IPv6 回环、链路本地、唯一本地地址
     * </p>
     *
     * @param ip 已规范化的 IP
     * @return 是否为内网地址
     */
    public static boolean isInternalIp(String ip) {
        if (!isValidIp(ip)) {
            return false;
        }
        String value = ip.trim().toLowerCase();
        if (!value.contains(":")) {
            return isInternalIpv4(value);
        }
        return isInternalIpv6(value);
    }

    // endregion

    // region 内网判断辅助方法
    // ===================================
    // 内网判断辅助方法
    // ===================================

    /**
     * 判断 IPv4 是否为内网地址
     *
     * @param ip IPv4 地址
     * @return 是否为内网
     */
    private static boolean isInternalIpv4(String ip) {
        String[] parts = ip.split("\\.");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        if (first == 10 || first == 127) {
            return true;
        }
        if (first == 192 && second == 168) {
            return true;
        }
        return first == 172 && second >= 16 && second <= 31;
    }

    /**
     * 判断 IPv6 是否为内网地址
     *
     * @param ip IPv6 地址
     * @return 是否为内网
     */
    private static boolean isInternalIpv6(String ip) {
        String lower = ip.toLowerCase();
        // 未指定地址与回环地址
        if ("::".equals(lower) || "::1".equals(lower) || "0:0:0:0:0:0:0:1".equals(lower)) {
            return true;
        }
        // 链路本地 fe80::/10
        if (lower.startsWith("fe8") || lower.startsWith("fe9")
                || lower.startsWith("fea") || lower.startsWith("feb")) {
            return true;
        }
        // 唯一本地地址 fc00::/7
        return lower.startsWith("fc") || lower.startsWith("fd");
    }

    // endregion

    // region 字符串辅助方法
    // ===================================
    // 字符串辅助方法
    // ===================================

    /**
     * 判断字符串是否为空或 unknown
     *
     * @param value 字符串
     * @return 是否为空或 unknown
     */
    private static boolean isBlankOrUnknown(String value) {
        return isBlank(value) || UNKNOWN.equalsIgnoreCase(value.trim());
    }

    /**
     * 判断字符串是否为空
     *
     * @param value 字符串
     * @return 是否为空
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 截断 IP 到数据库字段长度
     *
     * @param ip IP
     * @return 截断后的 IP
     */
    private static String truncate(String ip) {
        return ip.length() <= MAX_IP_LENGTH ? ip : ip.substring(0, MAX_IP_LENGTH);
    }

    // endregion
}
