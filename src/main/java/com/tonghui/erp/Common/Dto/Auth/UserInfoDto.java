package com.tonghui.erp.Common.Dto.Auth;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

/**
 * 当前用户信息响应数据传输对象
 * <p>
 * 用于返回当前登录用户的详细信息，包括按钮权限、角色列表、用户基本信息等，
 * 与前端openapi中的UserInfo接口对齐
 * </p>
 */
@Data
public class UserInfoDto {

    /**
     * 当前用户拥有的按钮权限编码列表
     */
    private List<String> buttons = new ArrayList<>();

    /**
     * 当前用户的角色编码列表
     */
    private List<String> roles = new ArrayList<>();

    /**
     * 用户唯一标识
     */
    private String userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 用户头像URL
     */
    private String avatar;
}
