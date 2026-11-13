package com.tonghui.erp.Common.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 客户端 MAC 地址工具类
 * <p>
 * 浏览器无法直接读取 MAC 地址，需要客户端（桌面端、本地代理或终端采集程序）
 * 通过请求头 X-Client-Mac 上报；服务端统一校验并格式化为 AA-BB-CC-DD-EE-FF。
 * 该值由客户端自行申报，仅用于审计辅助，不能作为身份认证依据。
 * </p>
 */
public final class ClientMacUtils {

    // region 常量
    // ===================================
    // 常量
    // ===================================

    /** 客户端 MAC 地址请求头（推荐） */
    public static final String HEADER_NAME = "X-Client-Mac";

    /** 兼容的 MAC 地址请求头，按优先级排列 */
    private static final String[] FALLBACK_HEADERS = {
            "X-Mac-Address",
            "Mac-Address"
    };

    /** MAC 地址十六进制长度 */
    private static final int MAC_HEX_LENGTH = 12;

    /** MAC 地址最大长度（AA-BB-CC-DD-EE-FF） */
    public static final int MAX_MAC_LENGTH = 17;

    /** 完整 MAC 地址格式：裸十六进制、短横线/冒号分隔、点分三段 */
    private static final Pattern FULL_MAC_PATTERN = Pattern.compile(
            "^([0-9A-Fa-f]{12}"
                    + "|([0-9A-Fa-f]{2}[-:]){5}[0-9A-Fa-f]{2}"
                    + "|[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4})$");

    /** 查询关键字允许的字符：十六进制、分隔符与空格 */
    private static final Pattern QUERY_PATTERN = Pattern.compile("^[0-9A-Fa-f:.\\- ]{1,32}$");

    // endregion

    // region 构造方法
    // ===================================
    // 构造方法
    // ===================================

    /**
     * 工具类禁止实例化
     */
    private ClientMacUtils() {
    }

    // endregion

    // region 请求头采集
    // ===================================
    // 请求头采集
    // ===================================

    /**
     * 从请求头采集并规范化客户端 MAC 地址
     *
     * @param request 当前 HTTP 请求
     * @return 规范化 MAC；请求为空或未上报合法 MAC 时返回 null
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String mac = normalize(request.getHeader(HEADER_NAME));
        if (mac != null) {
            return mac;
        }
        // 兼容旧客户端或代理转发的其他头名
        for (String header : FALLBACK_HEADERS) {
            mac = normalize(request.getHeader(header));
            if (mac != null) {
                return mac;
            }
        }
        return null;
    }

    // endregion

    // region 规范化与校验
    // ===================================
    // 规范化与校验
    // ===================================

    /**
     * 规范化完整 MAC 地址
     * <p>
     * 支持 AA-BB-CC-DD-EE-FF、AA:BB:CC:DD:EE:FF、AABB.CCDD.EEFF 与 12 位裸十六进制
     * </p>
     *
     * @param rawMac 原始 MAC
     * @return 规范化后的 AA-BB-CC-DD-EE-FF；不合法时返回 null
     */
    public static String normalize(String rawMac) {
        if (rawMac == null) {
            return null;
        }
        String value = rawMac.trim();
        if (!FULL_MAC_PATTERN.matcher(value).matches()) {
            return null;
        }
        String hex = stripSeparators(value);
        StringBuilder builder = new StringBuilder(MAX_MAC_LENGTH);
        for (int i = 0; i < MAC_HEX_LENGTH; i += 2) {
            if (i > 0) {
                builder.append('-');
            }
            builder.append(hex, i, i + 2);
        }
        return builder.toString();
    }

    /**
     * 规范化查询关键字
     * <p>
     * 查询时允许只输入 MAC 的一部分（如后 8 位），返回去除分隔符的大写十六进制串
     * </p>
     *
     * @param rawMac 原始查询关键字
     * @return 大写十六进制串；关键字为空、含非法字符或超过 12 位时返回 null
     */
    public static String normalizeQuery(String rawMac) {
        if (rawMac == null) {
            return null;
        }
        String value = rawMac.trim();
        if (!QUERY_PATTERN.matcher(value).matches()) {
            return null;
        }
        String hex = stripSeparators(value);
        if (hex.isEmpty() || hex.length() > MAC_HEX_LENGTH) {
            return null;
        }
        return hex;
    }

    /**
     * 去除 MAC 地址中的分隔符与空白并转大写
     *
     * @param value 原始值
     * @return 大写十六进制串
     */
    private static String stripSeparators(String value) {
        return value.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
    }

    // endregion
}
