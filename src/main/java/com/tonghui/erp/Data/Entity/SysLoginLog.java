package com.tonghui.erp.Data.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志表
 * <p>
 * 按账号记录每次登录尝试的 IP、归属地、时间与结果。
 * 日志只增不改，不继承审计基类，也不参与全局软删除。
 * </p>
 *
 * @TableName sys_login_log
 */
@TableName(value = "sys_login_log")
@Data
public class SysLoginLog {

    // region 基本信息字段
    // ===================================
    // 基本信息字段
    // ===================================

    /**
     * 日志ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（登录失败时可能为空）
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 登录账号（失败时记录尝试登录的账号）
     */
    @TableField(value = "user_account")
    private String userAccount;

    /**
     * 用户姓名（成功时快照）
     */
    @TableField(value = "user_name")
    private String userName;

    /**
     * 角色ID快照，逗号分隔
     */
    @TableField(value = "role_ids")
    private String roleIds;

    /**
     * 角色名称快照，顿号分隔
     */
    @TableField(value = "role_names")
    private String roleNames;

    // endregion

    // region 权限范围字段
    // ===================================
    // 权限范围字段
    // ===================================

    /**
     * 可访问菜单数
     */
    @TableField(value = "menu_count")
    private Integer menuCount;

    /**
     * 菜单权限键快照，逗号分隔
     */
    @TableField(value = "menu_keys")
    private String menuKeys;

    /**
     * 菜单名称快照，顿号分隔
     */
    @TableField(value = "menu_names")
    private String menuNames;

    /**
     * 接口权限数
     */
    @TableField(value = "permission_count")
    private Integer permissionCount;

    /**
     * 接口权限键快照，逗号分隔
     */
    @TableField(value = "permission_keys")
    private String permissionKeys;

    /**
     * 接口权限名称快照，顿号分隔
     */
    @TableField(value = "permission_names")
    private String permissionNames;

    // region 登录结果字段
    // ===================================
    // 登录结果字段
    // ===================================

    /**
     * 类型：LOGIN/LOGOUT
     */
    @TableField(value = "login_type")
    private String loginType;

    /**
     * 结果：1成功/0失败
     */
    @TableField(value = "login_result")
    private Integer loginResult;

    /**
     * 失败原因
     */
    @TableField(value = "fail_reason")
    private String failReason;

    // endregion

    // region 客户端信息字段
    // ===================================
    // 客户端信息字段
    // ===================================

    /**
     * 登录IP（IPv4/IPv6原样记录）
     */
    @TableField(value = "login_ip")
    private String loginIp;

    /**
     * IP归属地，如 江苏省南京市/内网/未知
     */
    @TableField(value = "ip_location")
    private String ipLocation;

    /**
     * 客户端User-Agent
     */
    @TableField(value = "user_agent")
    private String userAgent;

    /**
     * 登录时间
     */
    @TableField(value = "login_time")
    private LocalDateTime loginTime;

    // endregion
}
