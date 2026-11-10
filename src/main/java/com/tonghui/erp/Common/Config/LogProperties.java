package com.tonghui.erp.Common.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志配置
 * <p>
 * 对应 application.yml 中的 log.* 配置，控制 IP 代理头信任、归属地库路径、
 * 操作日志参数截断长度与登录日志开关
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "log")
@Data
public class LogProperties {

    // region 子配置
    // ===================================
    // 子配置
    // ===================================

    /**
     * IP 获取配置
     */
    private Ip ip = new Ip();

    /**
     * IP 归属地库配置
     */
    private Location location = new Location();

    /**
     * 操作日志配置
     */
    private Operation operation = new Operation();

    /**
     * 数据变更日志配置
     */
    private Change change = new Change();

    /**
     * 登录日志配置
     */
    private Login login = new Login();

    // endregion

    // region IP 配置
    // ===================================
    // IP 配置
    // ===================================

    /**
     * IP 获取配置
     */
    @Data
    public static class Ip {

        /**
         * 是否信任 X-Forwarded-For / X-Real-IP 等代理头
         * <p>只有部署在自家 Nginx 后面时才应开启；开启后 Nginx 必须覆盖该头，避免客户端伪造</p>
         */
        private boolean trustProxyHeaders = true;
    }

    // endregion

    // region 归属地库配置
    // ===================================
    // 归属地库配置
    // ===================================

    /**
     * IP 归属地库配置
     */
    @Data
    public static class Location {

        /**
         * IPv4 xdb 路径，支持 classpath: 前缀或绝对路径
         */
        private String v4XdbPath = "classpath:ip2region/ip2region_v4.xdb";

        /**
         * IPv6 xdb 路径，支持 classpath: 前缀或绝对路径
         */
        private String v6XdbPath = "classpath:ip2region/ip2region_v6.xdb";
    }

    // endregion

    // region 操作日志配置
    // ===================================
    // 操作日志配置
    // ===================================

    /**
     * 操作日志配置
     */
    @Data
    public static class Operation {

        /**
         * 操作日志总开关（数据库配置 log.operation.enabled 优先）
         */
        private boolean enabled = true;

        /**
         * 请求参数摘要最大长度，超出部分截断
         */
        private int maxParamLength = 2000;
    }

    // endregion

    // region 数据变更日志配置
    // ===================================
    // 数据变更日志配置
    // ===================================

    /**
     * 数据变更日志配置
     */
    @Data
    public static class Change {

        /**
         * 是否记录数据变更日志
         */
        private boolean enabled = true;

        /**
         * 数据变更日志默认保留天数（数据库配置优先）
         */
        private int retentionDays = 180;

        /**
         * 单次操作最多记录多少条变更
         */
        private int maxChanges = 100;

        /**
         * wrapper 批量更新时最多抓取多少行快照
         */
        private int maxRows = 1000;

        /**
         * 单个字段值最大字符数
         */
        private int maxValueLength = 2000;

        /**
         * 不记录变更的表
         */
        private List<String> excludeTables = new ArrayList<>(List.of(
                "sys_login_log",
                "sys_operation_log",
                "sys_data_change_log",
                "file_operation_log",
                "file_info",
                "business_file",
                "plan_status_log",
                "acceptance_resend_log"
        ));
    }

    // endregion

    // region 登录日志配置
    // ===================================
    // 登录日志配置
    // ===================================

    /**
     * 登录日志配置
     */
    @Data
    public static class Login {

        /**
         * 是否记录登录成功日志
         */
        private boolean recordSuccess = true;

        /**
         * 是否记录登录失败日志
         */
        private boolean recordFailures = true;
    }

    // endregion
}
