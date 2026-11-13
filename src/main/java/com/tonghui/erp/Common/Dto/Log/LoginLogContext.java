package com.tonghui.erp.Common.Dto.Log;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 登录日志上下文
 * <p>
 * 由 Controller 在请求线程中采集 IP、User-Agent、时间等信息，
 * 再交给异步服务落库，避免异步线程丢失请求上下文
 * </p>
 */
@Data
public class LoginLogContext {

    // region 用户字段
    // ===================================
    // 用户字段
    // ===================================

    /**
     * 用户ID（登录失败时可能为空）
     */
    private Long userId;

    /**
     * 登录账号（失败时记录尝试的账号）
     */
    private String userAccount;

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 角色ID快照
     */
    private List<Long> roleIds = new ArrayList<>();

    /**
     * 角色名称快照
     */
    private List<String> roleNames = new ArrayList<>();

    // endregion

    // region 权限范围字段
    // ===================================
    // 权限范围字段
    // ===================================

    /**
     * 菜单权限键快照
     */
    private List<String> menuKeys = new ArrayList<>();

    /**
     * 菜单名称快照
     */
    private List<String> menuNames = new ArrayList<>();

    /**
     * 接口权限键快照
     */
    private List<String> permissionKeys = new ArrayList<>();

    /**
     * 接口权限名称快照
     */
    private List<String> permissionNames = new ArrayList<>();

    // endregion

    // region 结果字段
    // ===================================
    // 结果字段
    // ===================================

    /**
     * 类型：LOGIN/LOGOUT
     */
    private String loginType = "LOGIN";

    /**
     * 结果：1成功/0失败
     */
    private Integer loginResult = 1;

    /**
     * 失败原因
     */
    private String failReason;

    // endregion

    // region 客户端字段
    // ===================================
    // 客户端字段
    // ===================================

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * 客户端MAC地址（请求头 X-Client-Mac 上报）
     */
    private String macAddress;

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

    // region 便捷方法
    // ===================================
    // 便捷方法
    // ===================================

    /**
     * 标记登录成功
     *
     * @param userId      用户ID
     * @param userAccount 登录账号
     * @param userName    用户姓名
     */
    public void markSuccess(Long userId, String userAccount, String userName) {
        this.userId = userId;
        this.userAccount = userAccount;
        this.userName = userName;
        this.loginResult = 1;
        this.failReason = null;
    }

    /**
     * 标记登录失败
     *
     * @param userId     用户ID（可为空）
     * @param failReason 失败原因
     */
    public void markFail(Long userId, String failReason) {
        this.userId = userId;
        this.loginResult = 0;
        this.failReason = failReason;
    }

    // endregion
}
