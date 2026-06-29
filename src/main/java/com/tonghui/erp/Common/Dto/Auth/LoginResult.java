package com.tonghui.erp.Common.Dto.Auth;

import com.tonghui.erp.Data.Entity.Role;
import com.tonghui.erp.Data.Entity.User;
import lombok.Data;
import java.util.List;

/**
 * 登录结果类
 * <p>
 * 包含登录操作的基础结果信息，用于封装用户登录后的基础响应数据
 * </p>
 */
@Data
public class LoginResult {
    
    //#region 登录结果基础字段
    // ===================================
    // 登录结果基础字段
    // ===================================
    
    /**
     * 登录是否成功
     * <p>标识用户登录操作是否成功</p>
     */
    private boolean success;
        
    /**
     * 访问令牌
     * <p>用于后续请求身份验证的 JWT 令牌</p>
     */
    private String token;
        
    /**
     * 刷新令牌
     * <p>用于刷新访问令牌的长期有效令牌</p>
     */
    private String refreshToken;
        
    /**
     * 用户实体
     * <p>登录用户的基本信息实体</p>
     */
    private User user;
    
    /**
     * 用户角色列表
     * <p>登录用户拥有的所有角色实体</p>
     */
    private List<Role> roles;
    
    /**
     * 响应消息
     * <p>登录结果的描述信息</p>
     */
    private String message;
    
    //#endregion
    
    //#region 内部数据类
    // ===================================
    // 内部数据类
    // ===================================
    
    /**
     * 用于API响应的数据包装类，避免字段重复
     * <p>专为API接口响应格式设计的数据包装类</p>
     */
    @Data
    public static class LoginResultData {
        
        //#region 结果数据字段
        // ===================================
        // 结果数据字段
        // ===================================
        
        /**
         * 访问令牌
         * <p>用于后续请求身份验证的 JWT 令牌</p>
         */
        private String token;
                
        /**
         * 刷新令牌
         * <p>用于刷新访问令牌的长期有效令牌</p>
         */
        private String refreshToken;
                
        /**
         * 用户实体
         * <p>登录用户的基本信息实体</p>
         */
        private User user;
        
        /**
         * 用户角色列表
         * <p>登录用户拥有的所有角色实体</p>
         */
        private List<Role> roles;
        
        //#endregion
        
        //#region 构造方法
        // ===================================
        // 构造方法
        // ===================================
        
        /**
         * 基于 LoginResult 构造 LoginResultData 对象
         * 
         * @param result LoginResult 对象
         */
        public LoginResultData(LoginResult result) {
            this.token = result.getToken();
            this.refreshToken = result.getRefreshToken();
            this.user = result.getUser();
            this.roles = result.getRoles();
        }
        
        //#endregion
    }
    
    //#endregion
}
