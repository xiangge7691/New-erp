package com.tonghui.erp.Common.Dto.Auth;

import com.tonghui.erp.Common.Dto.System.PermissionDto;
import com.tonghui.erp.Common.Dto.System.RoleDto;
import com.tonghui.erp.Common.Dto.System.UserDto;
import lombok.Data;

import java.util.List;
import java.util.ArrayList;

/**
 * 登录响应类
 * <p>
 * 包含完整的用户信息、角色和权限信息，用于封装用户登录后的响应数据
 * </p>
 */
@Data
public class LoginResponse {
    
    //#region 响应基本信息字段
    // ===================================
    // 响应基本信息字段
    // ===================================
    
    /**
     * 登录是否成功
     * <p>标识用户登录操作是否成功</p>
     */
    private boolean success;
    
    /**
     * 响应消息
     * <p>登录结果的描述信息</p>
     */
    private String message;
    
    /**
     * 登录数据
     * <p>包含登录成功后的用户详细信息</p>
     */
    private LoginData data = new LoginData();
    
    /**
     * 响应状态码
     * <p>HTTP响应状态码</p>
     */
    private int code;
    
    //#endregion
    
    //#region 内部数据类
    // ===================================
    // 内部数据类
    // ===================================
    
    /**
     * 登录数据内部类
     * <p>封装登录成功后的详细用户信息</p>
     */
    @Data
    public static class LoginData {
        
        //#region 登录数据字段
        // ===================================
        // 登录数据字段
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
         * 用户信息
         * <p>登录用户的基本信息</p>
         */
        private UserDto user;
        
        /**
         * 用户角色列表
         * <p>登录用户拥有的所有角色</p>
         */
        private List<RoleDto> roles = new ArrayList<>();
        
        /**
         * 权限树
         * <p>用户拥有的权限信息，以树形结构组织</p>
         */
        private List<PermissionDto> permissionTree = new ArrayList<>();
        
        //#endregion
    }
    
    /**
     * 用于API响应的数据包装类，避免字段重复
     * <p>专为API接口响应格式设计的数据包装类</p>
     */
    @Data
    public static class LoginResponseData {
        
        //#region 响应数据字段
        // ===================================
        // 响应数据字段
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
         * 用户信息
         * <p>登录用户的基本信息</p>
         */
        private UserDto user;
        
        /**
         * 用户角色列表
         * <p>登录用户拥有的所有角色</p>
         */
        private List<RoleDto> roles;
        
        /**
         * 权限树
         * <p>用户拥有的权限信息，以树形结构组织</p>
         */
        private List<PermissionDto> permissionTree;
        
        //#endregion
        
        //#region 构造方法
        // ===================================
        // 构造方法
        // ===================================
        
        /**
         * 基于 LoginResponse 构造 LoginResponseData 对象
         * 
         * @param response LoginResponse 对象
         */
        public LoginResponseData(LoginResponse response) {
            this.token = response.getData().getToken();
            this.refreshToken = response.getData().getRefreshToken();
            this.user = response.getData().getUser();
            this.roles = response.getData().getRoles();
            this.permissionTree = response.getData().getPermissionTree();
        }
        
        //#endregion
    }
    
    //#endregion
}
