package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Auth.AuthLoginResponse;
import com.tonghui.erp.Common.Dto.Auth.LoginResult;
import com.tonghui.erp.Common.Dto.Auth.LoginResponse;

import java.util.concurrent.CompletableFuture;

/**
 * 登录服务接口
 * <p>
 * 定义用户登录相关业务操作的接口规范，提供用户身份验证和登录信息构建功能
 * </p>
 * 
 * @author tonghui
 * @since 2025-08-27
 */
public interface LoginService {
    
    //#region 异步登录接口
    // ===================================
    // 异步登录接口
    // ===================================
    
    /**
     * 用户登录并构建完整响应
     * <p>
     * 在基础登录验证的基础上，整合用户的完整信息，包括角色、权限等
     * 该方法使用异步方式处理，提高系统响应性能
     * </p>
     *
     * @param userName 用户名，用于标识用户身份的唯一字符串
     * @param password 密码，用户登录凭证的明文形式
     * @return 返回登录响应对象，包含验证状态、访问令牌、用户详细信息、角色列表和权限树
     */
    CompletableFuture<LoginResponse> loginWithFullResponseAsync(String userName, String password);
    //#endregion
    
    //#region 同步登录接口
    // ===================================
    // 同步登录接口
    // ===================================
    
    /**
     * 用户登录并构建完整响应（同步版本）
     * <p>
     * 在基础登录验证的基础上，整合用户的完整信息，包括角色、权限等
     * 该方法使用同步方式处理，适用于不需要异步处理的场景
     * </p>
     *
     * @param userName 用户名，用于标识用户身份的唯一字符串
     * @param password 密码，用户登录凭证的明文形式
     * @return 返回登录响应对象，包含验证状态、访问令牌、用户详细信息、角色列表和权限树
     */
    LoginResponse loginWithFullResponse(String userName, String password);
    //#endregion
    
    //#region 令牌刷新接口
    // ===================================
    // 令牌刷新接口
    // ===================================
    
    /**
     * 刷新访问令牌
     * <p>
     * 使用刷新令牌获取新的访问令牌，实现无感刷新功能
     * 该方法会验证刷新令牌的有效性，如果有效则生成新的访问令牌和刷新令牌
     * </p>
     *
     * @param refreshToken 刷新令牌
     * @return 包含新访问令牌和刷新令牌的响应对象，刷新失败时返回 null
     */
    AuthLoginResponse refreshAccessToken(String refreshToken);
    //#endregion
}
