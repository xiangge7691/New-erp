package com.tonghui.erp.Common.Dto.Auth;

import lombok.Data;

/**
 * 登录请求参数
 * <p>
 * 用于封装用户登录时提交的请求参数，包括用户名和密码
 * </p>
 */
@Data
public class LoginRequest {
    
    //#region 登录凭证字段
    // ===================================
    // 登录凭证字段
    // ===================================
    
    /**
     * 用户名
     * <p>用户登录时使用的用户名</p>
     */
    private String userName;
    
    /**
     * 密码
     * <p>用户登录时使用的密码</p>
     */
    private String password;
    
    //#endregion
}
