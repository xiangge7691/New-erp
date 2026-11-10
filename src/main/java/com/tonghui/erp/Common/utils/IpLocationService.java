package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Common.Config.LogProperties;
import jakarta.annotation.PreDestroy;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.ConfigBuilder;
import org.lionsoul.ip2region.service.Ip2Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.StringJoiner;

/**
 * IP 归属地解析服务
 * <p>
 * 基于离线 ip2region 双库（IPv4 + IPv6）解析客户端 IP 归属地，
 * 内网地址直接返回“内网”，解析失败返回“未知”，不依赖外网接口
 * </p>
 */
@Component
public class IpLocationService {

    // region 常量与字段
    // ===================================
    // 常量与字段
    // ===================================

    private static final Logger log = LoggerFactory.getLogger(IpLocationService.class);

    /** classpath 前缀 */
    private static final String CLASSPATH_PREFIX = "classpath:";

    /** 归属地字段最大长度（与数据库 VARCHAR(128) 保持一致） */
    private static final int MAX_LOCATION_LENGTH = 120;

    /** 内网标识 */
    public static final String INTERNAL = "内网";

    /** 未知标识 */
    public static final String UNKNOWN = "未知";

    /**
     * ip2region 查询服务（IPv4 + IPv6）
     */
    private final Ip2Region ip2Region;

    // endregion

    // region 构造与初始化
    // ===================================
    // 构造与初始化
    // ===================================

    /**
     * 构造 IP 归属地服务，启动时加载 v4 / v6 离线库
     *
     * @param logProperties 日志配置
     */
    public IpLocationService(LogProperties logProperties) {
        this.ip2Region = init(logProperties.getLocation());
    }

    /**
     * 初始化双协议查询服务
     *
     * @param location 归属地库配置
     * @return 查询服务；两个库都不可用时返回 null
     */
    private Ip2Region init(LogProperties.Location location) {
        Config v4Config = loadConfig(location.getV4XdbPath(), true);
        Config v6Config = loadConfig(location.getV6XdbPath(), false);
        if (v4Config == null && v6Config == null) {
            log.warn("IP归属地库均未加载，IP归属地将统一显示为“未知”");
            return null;
        }
        try {
            Ip2Region service = Ip2Region.create(v4Config, v6Config);
            log.info("IP归属地库加载完成：IPv4={}，IPv6={}",
                    v4Config != null ? "已加载" : "未加载",
                    v6Config != null ? "已加载" : "未加载");
            return service;
        } catch (Exception e) {
            log.warn("IP归属地服务初始化失败，IP归属地将显示为“未知”：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 加载单个版本的 xdb 配置
     *
     * @param path xdb 路径，支持 classpath: 前缀或绝对路径
     * @param v4   是否 IPv4 库
     * @return 配置；文件不存在或解析失败时返回 null
     */
    private Config loadConfig(String path, boolean v4) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        try (InputStream inputStream = openStream(path.trim())) {
            if (inputStream == null) {
                log.warn("IP归属地库文件不存在：{}", path);
                return null;
            }
            // 从 jar 内读取 xdb 时必须使用全内存缓存
            ConfigBuilder builder = Config.custom()
                    .setCachePolicy(Config.BufferCache)
                    .setXdbInputStream(inputStream);
            return v4 ? builder.asV4() : builder.asV6();
        } catch (Exception e) {
            log.warn("IP归属地库加载失败：{} - {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 打开 xdb 输入流
     *
     * @param path 路径
     * @return 输入流；文件不存在时返回 null
     * @throws Exception 打开失败
     */
    private InputStream openStream(String path) throws Exception {
        if (path.startsWith(CLASSPATH_PREFIX)) {
            Resource resource = new ClassPathResource(path.substring(CLASSPATH_PREFIX.length()));
            return resource.exists() ? resource.getInputStream() : null;
        }
        File file = new File(path);
        return file.exists() ? new FileInputStream(file) : null;
    }

    /**
     * 应用关闭时释放查询服务
     */
    @PreDestroy
    public void destroy() {
        if (ip2Region == null) {
            return;
        }
        try {
            ip2Region.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("关闭IP归属地服务被中断");
        }
    }

    // endregion

    // region 归属地解析
    // ===================================
    // 归属地解析
    // ===================================

    /**
     * 解析 IP 归属地
     *
     * @param rawIp 原始 IP（支持 IPv4 / IPv6）
     * @return 归属地，如“江苏省南京市”；内网返回“内网”；无法解析返回“未知”
     */
    public String resolve(String rawIp) {
        String ip = IpUtils.normalize(rawIp);
        if (!IpUtils.isValidIp(ip)) {
            return UNKNOWN;
        }
        if (IpUtils.isInternalIp(ip)) {
            return INTERNAL;
        }
        if (ip2Region == null) {
            return UNKNOWN;
        }
        try {
            String region = ip2Region.search(ip);
            return formatRegion(region);
        } catch (Exception e) {
            log.debug("IP归属地解析失败：{} - {}", ip, e.getMessage());
            return UNKNOWN;
        }
    }

    /**
     * 将 ip2region 返回的原始区域串转换为展示值
     * <p>
     * 原始格式：国家|省份|城市|ISP|国家代码（如 中国|广东省|深圳市|电信|CN）
     * </p>
     *
     * @param region 原始区域串
     * @return 展示用归属地
     */
    private String formatRegion(String region) {
        if (region == null || region.trim().isEmpty()) {
            return UNKNOWN;
        }
        String[] parts = region.split("\\|", -1);
        String country = part(parts, 0);
        String province = part(parts, 1);
        String city = part(parts, 2);

        StringJoiner joiner;
        if ("中国".equals(country)) {
            joiner = new StringJoiner("");
            if (!province.isEmpty()) {
                joiner.add(province);
            }
            if (!city.isEmpty()) {
                joiner.add(city);
            }
            if (joiner.length() == 0) {
                joiner.add(country);
            }
        } else {
            // 境外地址用空格分隔国家、省州、城市，避免英文单词粘连
            joiner = new StringJoiner(" ");
            if (!country.isEmpty()) {
                joiner.add(country);
            }
            if (!province.isEmpty()) {
                joiner.add(province);
            }
            if (!city.isEmpty()) {
                joiner.add(city);
            }
        }

        String location = joiner.toString().trim();
        if (location.isEmpty()) {
            return UNKNOWN;
        }
        return location.length() <= MAX_LOCATION_LENGTH
                ? location
                : location.substring(0, MAX_LOCATION_LENGTH);
    }

    /**
     * 读取区域串的指定分段，过滤空值与占位 0
     *
     * @param parts 分段数组
     * @param index 下标
     * @return 分段值，不存在或为占位值时返回空串
     */
    private String part(String[] parts, int index) {
        if (index >= parts.length || parts[index] == null) {
            return "";
        }
        String value = parts[index].trim();
        return "0".equals(value) ? "" : value;
    }

    // endregion
}
