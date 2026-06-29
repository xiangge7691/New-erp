package com.tonghui.erp.Common.Dto.Auth;

import lombok.Data;

/**
 * 简化登录响应
 * 与 openapi 中 LoginResponse(token, refreshToken) 对齐
 */
@Data
public class AuthLoginResponse {
    private String token;
    private String refreshToken;
}