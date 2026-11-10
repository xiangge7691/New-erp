package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志查询结果
 * <p>
 * 用于登录日志列表展示，包含账号、登录时间、IP、归属地与结果
 * </p>
 */
@Data
public class LoginLogDto {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 登录账号
     */
    private String userAccount;

    /**
     * 用户姓名
     */
    private String userName;

    // endregion

    // region 登录结果字段
    // ===================================
    // 登录结果字段
    // ===================================

    /**
     * 类型：LOGIN/LOGOUT
     */
    private String loginType;

    /**
     * 结果：1成功/0失败
     */
    private Integer loginResult;

    /**
     * 失败原因
     */
    private String failReason;

    // endregion

    // region 客户端信息字段
    // ===================================
    // 客户端信息字段
    // ===================================

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * IP归属地
     */
    private String ipLocation;

    /**
     * 客户端User-Agent
     */
    private String userAgent;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    // endregion
}
