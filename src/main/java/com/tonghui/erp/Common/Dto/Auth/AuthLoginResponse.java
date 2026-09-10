package com.tonghui.erp.Common.Dto.Auth;

import lombok.Data;

/**
 * 登录响应数据传输对象
 * <p>
 * 用于登录成功后返回JWT令牌信息，包含访问令牌和刷新令牌，
 * 与前端openapi中的LoginResponse接口对齐
 * </p>
 */
@Data
public class AuthLoginResponse {

    /**
     * JWT访问令牌，用于API请求身份验证
     */
    private String token;

    /**
     * JWT刷新令牌，用于在访问令牌过期后获取新的访问令牌
     */
    private String refreshToken;
}
