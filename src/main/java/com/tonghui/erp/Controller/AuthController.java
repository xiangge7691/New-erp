package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Auth.AuthLoginResponse;
import com.tonghui.erp.Common.Dto.Auth.LoginRequest;
import com.tonghui.erp.Common.Dto.Auth.LoginResponse;
import com.tonghui.erp.Service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * 认证控制器
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/auth/login                      │ POST  │ 用户登录                            │
 * │ 2  │ /api/auth/refresh                    │ POST  │ 刷新访问令牌                        │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 *
 * 说明：
 * - 处理用户认证相关的HTTP请求
 * - 提供登录和令牌刷新功能
 * - 登录成功后返回访问令牌和刷新令牌
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    private final LoginService loginService;

    @Autowired
    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    // endregion

    // region 认证接口
    // ===================================
    // 认证接口
    // ===================================

    /**
     * 用户登录接口
     *
     * 根据用户名和密码进行身份验证，验证成功后返回访问令牌和刷新令牌。
     * 访问令牌用于访问受保护的API，刷新令牌用于在访问令牌过期时获取新的访问令牌。
     *
     * 示例请求：
     * POST /api/auth/login
     * Content-Type: application/json
     * {
     *   "userName": "root",
     *   "password": "root"
     * }
     *
     * @param loginRequest 登录请求参数，包含用户名和密码
     * @return 登录响应，包含访问令牌和刷新令牌
     */
    @PostMapping("/login")
    public ApiResponse<AuthLoginResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 调用登录服务进行身份验证
            CompletableFuture<LoginResponse> futureResponse = loginService.loginWithFullResponseAsync(
                    loginRequest.getUserName(),
                    loginRequest.getPassword());

            LoginResponse loginResponse = futureResponse.get();

            // 检查登录是否成功
            if (!loginResponse.isSuccess()) {
                return ApiResponse.errorResponse(loginResponse.getMessage());
            }

            // 构造符合 OpenAPI 规范的响应格式
            AuthLoginResponse authLoginResponse = new AuthLoginResponse();
            authLoginResponse.setToken(loginResponse.getData().getToken());
            authLoginResponse.setRefreshToken(loginResponse.getData().getRefreshToken());
            
            return ApiResponse.successResponse(authLoginResponse);
        } catch (Exception e) {
            return ApiResponse.errorResponse("登录过程中发生错误：" + e.getMessage());
        }
    }
    
    /**
     * 刷新访问令牌接口
     *
     * 使用刷新令牌获取新的访问令牌，实现无感刷新功能。
     * 刷新令牌必须有效且未过期才能成功刷新。
     *
     * 示例请求：
     * POST /api/auth/refresh?refreshToken=eyJhbGciOiJIUzI1NiJ9...
     *
     * @param refreshToken 刷新令牌
     * @return 登录响应，包含新的访问令牌和刷新令牌
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthLoginResponse> refreshToken(@RequestParam String refreshToken) {
        try {
            // 验证刷新令牌的有效性
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ApiResponse.errorResponse("刷新令牌不能为空");
            }
            
            // 调用登录服务刷新令牌
            AuthLoginResponse authLoginResponse = loginService.refreshAccessToken(refreshToken);
            
            // 检查刷新是否成功
            if (authLoginResponse == null || authLoginResponse.getToken() == null || authLoginResponse.getToken().isEmpty()) {
                return ApiResponse.errorResponse("刷新令牌无效或已过期");
            }
            
            return ApiResponse.successResponse(authLoginResponse);
        } catch (Exception e) {
            return ApiResponse.errorResponse("刷新令牌过程中发生错误：" + e.getMessage());
        }
    }

    // endregion
}