package com.tonghui.erp.Service.impl;

import com.tonghui.erp.Common.Dto.Auth.AuthLoginResponse;
import com.tonghui.erp.Common.Dto.Auth.LoginResponse;
import com.tonghui.erp.Common.Dto.Auth.LoginResult;
import com.tonghui.erp.Common.Dto.System.DepartmentDto;
import com.tonghui.erp.Common.Dto.System.PermissionDto;
import com.tonghui.erp.Common.Dto.System.RoleDto;
import com.tonghui.erp.Common.Dto.System.UserDto;
import com.tonghui.erp.Common.utils.JwtHelper;
import com.tonghui.erp.Common.utils.PasswordHasher;
import com.tonghui.erp.Common.Config.JwtConfig;
import com.tonghui.erp.Data.Entity.*;
import com.tonghui.erp.Service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 登录服务实现类
 * <p>
 * 提供用户身份验证、JWT令牌生成以及用户权限信息整合等业务逻辑处理
 * </p>
 */
@Service
public class LoginServiceImpl implements LoginService {

    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final UserRoleService userRoleService;
    private final UserDepartmentService userDepartmentService;
    private final RolePermService rolePermService;
    private final DepartmentService departmentService;
    private final JwtConfig jwtConfig;

    @Autowired
    public LoginServiceImpl(UserService userService,
                            RoleService roleService,
                            PermissionService permissionService,
                            UserRoleService userRoleService,
                            UserDepartmentService userDepartmentService,
                            RolePermService rolePermService,
                            DepartmentService departmentService,
                            JwtConfig jwtConfig) {
        this.userService = userService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.userRoleService = userRoleService;
        this.userDepartmentService = userDepartmentService;
        this.rolePermService = rolePermService;
        this.departmentService = departmentService;
        this.jwtConfig = jwtConfig;
    }

    //#region 异步登录接口实现
    // ===================================
    // 异步登录接口实现
    // ===================================
    
    /**
     * 用户登录并构建完整响应
     *
     * @param userName 用户名
     * @param password 密码
     * @return 包含完整用户信息的登录响应对象
     */
    @Override
    public CompletableFuture<LoginResponse> loginWithFullResponseAsync(String userName, String password) {
        return CompletableFuture.supplyAsync(() -> loginWithFullResponse(userName, password));
    }
    //#endregion

    //#region 同步登录接口实现
    // ===================================
    // 同步登录接口实现
    // ===================================
    
    /**
     * 用户登录并构建完整响应（同步版本）
     * 在基础登录验证的基础上，整合用户的完整信息，包括角色、权限等
     *
     * @param userName 用户名，用于标识用户身份的唯一字符串
     * @param password 密码，用户登录凭证的明文形式
     * @return 返回登录响应对象，包含验证状态、访问令牌、用户详细信息、角色列表和权限树
     */
    @Override
    public LoginResponse loginWithFullResponse(String userName, String password) {
        try {
            // 执行基础登录验证
            LoginResult loginResult = login(userName, password);

            // 如果登录验证失败，返回错误响应
            if (!loginResult.isSuccess()) {
                LoginResponse response = new LoginResponse();
                response.setSuccess(false);
                response.setMessage(loginResult.getMessage());
                response.setCode(401);
                return response;
            }

            // 获取用户角色
            List<UserRole> userRoles = userRoleService.list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserRole>()
                    .eq("user_id", loginResult.getUser().getUserId()));

            List<Long> roleIds = userRoles.stream()
                    .map(UserRole::getRoleId)
                    .collect(Collectors.toList());

            List<Role> roles = new ArrayList<>();
            if (!roleIds.isEmpty()) {
                roles = roleService.list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Role>()
                        .in("role_id", roleIds)
                        .eq("role_status", 1)); // 只获取启用状态的角色
            }

            // 转换角色为RoleDto对象
            List<RoleDto> roleDtos = roles.stream().map(this::convertRoleToDto).collect(Collectors.toList());

            // 获取用户权限（去重）
            Set<Long> permissionIds = new HashSet<>();
            List<Permission> permissions = new ArrayList<>();

            // 只获取启用状态角色的权限
            for (Role role : roles) {
                List<Long> permIds = rolePermService.list(
                                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RolePerm>()
                                        .eq("role_id", role.getRoleId()))
                        .stream()
                        .map(RolePerm::getPermId)
                        .collect(Collectors.toList());

                if (!permIds.isEmpty()) {
                    List<Permission> rolePermissions = permissionService.list(
                            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Permission>()
                                    .in("perm_id", permIds));

                    for (Permission perm : rolePermissions) {
                        if (permissionIds.add(perm.getPermId())) {
                            permissions.add(perm);
                        }
                    }
                }
            }

            // 构建权限树
            List<PermissionDto> permissionTree = buildPermissionTree(permissions);

            // 获取用户部门信息
            List<DepartmentDto> departmentDtos = getUserDepartments(loginResult.getUser().getUserId());

            // 构建并返回完整登录响应
            LoginResponse response = new LoginResponse();
            UserDto userDto = buildUserDto(loginResult.getUser());
            userDto.setDepartments(departmentDtos);

            LoginResponse.LoginData data = new LoginResponse.LoginData();
            data.setToken(loginResult.getToken());
            data.setRefreshToken(loginResult.getRefreshToken());
            data.setUser(userDto);
            data.setRoles(roleDtos);
            data.setPermissionTree(permissionTree);

            response.setData(data);
            response.setMessage(loginResult.getMessage());
            response.setSuccess(true);
            response.setCode(200);

            return response;
        } catch (Exception ex) {
            // 处理异常情况，返回错误响应
            LoginResponse response = new LoginResponse();
            response.setSuccess(false);
            response.setMessage("登录过程中发生错误: " + ex.getMessage());
            response.setCode(500);
            return response;
        }
    }
    //#endregion

    //#region 内部辅助方法
    // ===================================
    // 内部辅助方法
    // ===================================
    
    /**
     * 用户登录验证（内部使用）
     * 验证用户凭据并生成JWT访问令牌
     *
     * @param userName 用户名
     * @param password 密码
     * @return 包含验证结果、用户信息和访问令牌的登录结果对象
     */
    private LoginResult login(String userName, String password) {
        // 初始化登录结果对象
        LoginResult result = new LoginResult();

        // 检查用户名和密码是否为空
        if (userName == null || userName.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            result.setSuccess(false);
            result.setMessage("用户名或密码不能为空");
            return result;
        }

        // 获取用户信息（使用精确查询）
        User user = userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("user_account", userName));

        // 如果没有找到用户
        if (user == null) {
            result.setSuccess(false);
            result.setMessage("用户名或密码错误");
            return result;
        }

        // 验证用户状态
        if (user.getUserStatus() == 0) {
            result.setSuccess(false);
            result.setMessage("用户账户已被禁用");
            return result;
        }

        // 验证密码
        if (!PasswordHasher.verifyPassword(password, user.getPassword())) {
            result.setSuccess(false);
            result.setMessage("用户名或密码错误");
            return result;
        }

        // 获取用户角色
        List<UserRole> userRoles = userRoleService.list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserRole>()
                .eq("user_id", user.getUserId()));

        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());

        List<Role> roles = new ArrayList<>();
        if (!roleIds.isEmpty()) {
            roles = roleService.list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Role>()
                    .in("role_id", roleIds));
        }

        // 生成 JWT 访问令牌
        String[] roleNames = roles.stream()
                .map(Role::getRoleName)
                .toArray(String[]::new);
        
        String token = JwtHelper.generateToken(
                String.valueOf(user.getUserId()),
                user.getUserName(),
                roleNames,
                jwtConfig.getSecretKey(),
                jwtConfig.getIssuer(),
                jwtConfig.getAudience(),
                jwtConfig.getExpiresInMinutes()
        );
                
        // 生成刷新令牌（使用更长的有效期）
        String refreshToken = JwtHelper.generateToken(
                String.valueOf(user.getUserId()),
                user.getUserName(),
                roleNames,
                jwtConfig.getSecretKey(),
                jwtConfig.getIssuer(),
                jwtConfig.getAudience(),
                jwtConfig.getRefreshExpiresInMinutes()
        );
        
        result.setSuccess(true);
        result.setToken(token);
        result.setRefreshToken(refreshToken);
        result.setUser(user);
        result.setRoles(roles);
        result.setMessage("登录成功");
        return result;
    }

    /**
     * 将User实体转换为UserDto，屏蔽密码字段
     *
     * @param user User实体
     * @return UserDto对象
     */
    private UserDto buildUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getUserId());
        dto.setUserName(user.getUserAccount());
        dto.setName(user.getUserName());
        dto.setPhone(user.getPhone());
        dto.setGender(user.getGender());
        dto.setStatus(user.getUserStatus());
        dto.setNotes(user.getUserNotes());
        dto.setCreatedTime(user.getCreatedTime());
        dto.setUpdateTime(user.getUpdateTime());
        return dto;
    }

    /**
     * 获取用户部门信息
     *
     * @param userId 用户ID
     * @return 部门DTO列表
     */
    private List<DepartmentDto> getUserDepartments(Long userId) {
        List<com.tonghui.erp.Data.Entity.UserDepartment> userDepartments = userDepartmentService.list(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.tonghui.erp.Data.Entity.UserDepartment>()
                        .eq("user_id", userId));

        List<DepartmentDto> departmentDtos = new ArrayList<>();
        for (com.tonghui.erp.Data.Entity.UserDepartment userDepartment : userDepartments) {
            Department department = departmentService.getById(userDepartment.getDepartmentId());
            if (department != null) {
                DepartmentDto departmentDto = new DepartmentDto();
                departmentDto.setId(department.getDepartmentId());
                departmentDto.setDepartmentName(department.getDepartmentName());
                departmentDtos.add(departmentDto);
            }
        }
        return departmentDtos;
    }

    /**
     * 将Role实体转换为RoleDto
     *
     * @param role Role实体
     * @return RoleDto对象
     */
    private RoleDto convertRoleToDto(Role role) {
        RoleDto dto = new RoleDto();
        dto.setRoleId(role.getRoleId());
        dto.setRoleName(role.getRoleName());
        dto.setRoleDesc(role.getRoleDesc());
        dto.setStatus(role.getRoleStatus());
        dto.setCreateTime(role.getCreateTime());
        dto.setUpdateTime(role.getUpdateTime());
        return dto;
    }

    /**
     * 将Permission实体转换为PermissionDto
     *
     * @param permission Permission实体
     * @return PermissionDto对象
     */
    private PermissionDto convertPermissionToDto(Permission permission) {
        PermissionDto dto = new PermissionDto();
        dto.setId(permission.getPermId());
        dto.setPermKey(permission.getPermKey());
        dto.setPermName(permission.getPermName());
        dto.setPermType((String) permission.getPermType());
        dto.setParentId(permission.getParentId());
        dto.setDisplayOrder(permission.getDisplayOrder());
        dto.setStatus(permission.getPermStatus());
        return dto;
    }

    /**
     * 构建权限树结构
     *
     * @param permissions 权限列表
     * @return 权限树
     */
    private List<PermissionDto> buildPermissionTree(List<Permission> permissions) {
        // 将Permission实体转换为PermissionDto
        List<PermissionDto> permissionDtos = permissions.stream()
                .map(this::convertPermissionToDto)
                .collect(Collectors.toList());

        // 构建树结构
        Map<Long, PermissionDto> permissionMap = new HashMap<>();
        List<PermissionDto> rootPermissions = new ArrayList<>();

        // 建立映射关系
        for (PermissionDto dto : permissionDtos) {
            permissionMap.put(dto.getId(), dto);
        }

        // 构建父子关系
        for (PermissionDto dto : permissionDtos) {
            if (dto.getParentId() != null && permissionMap.containsKey(dto.getParentId())) {
                permissionMap.get(dto.getParentId()).getChildren().add(dto);
            } else {
                rootPermissions.add(dto);
            }
        }

        // 对所有层级的子权限进行排序
        sortPermissionChildren(rootPermissions);

        return rootPermissions;
    }

    /**
     * 递归对权限的子权限进行排序
     *
     * @param permissions 权限集合
     */
    private void sortPermissionChildren(List<PermissionDto> permissions) {
        for (PermissionDto perm : permissions) {
            if (!perm.getChildren().isEmpty()) {
                // 按显示顺序排序
                perm.getChildren().sort(Comparator.comparing(PermissionDto::getDisplayOrder));
                // 递归排序子权限的子权限
                sortPermissionChildren(perm.getChildren());
            }
        }
    }
    //#endregion
    
    //#region 令牌刷新方法
    // ===================================
    // 令牌刷新方法
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
    @Override
    public AuthLoginResponse refreshAccessToken(String refreshToken) {
        try {
            // 验证刷新令牌
            boolean isValid = JwtHelper.validateToken(
                    refreshToken,
                    jwtConfig.getSecretKey(),
                    jwtConfig.getIssuer(),
                    jwtConfig.getAudience()
            );
            
            if (!isValid) {
                return null;
            }
            
            // 从刷新令牌中提取用户信息
            Map<String, Object> userInfo = JwtHelper.getUserInfoFromRefreshToken(
                    refreshToken,
                    jwtConfig.getSecretKey()
            );
            
            // 检查必要信息是否存在
            String userId = (String) userInfo.get("userId");
            String username = (String) userInfo.get("username");
            List<String> roles = (List<String>) userInfo.get("roles");
            
            if (userId == null || username == null) {
                return null;
            }
            
            // 生成新的访问令牌（使用较短的有效期）
            String newAccessToken = JwtHelper.generateToken(
                    userId,
                    username,
                    roles != null ? roles.toArray(new String[0]) : new String[0],
                    jwtConfig.getSecretKey(),
                    jwtConfig.getIssuer(),
                    jwtConfig.getAudience(),
                    jwtConfig.getExpiresInMinutes()
            );
            
            // 生成新的刷新令牌（使用较长的有效期）
            String newRefreshToken = JwtHelper.generateToken(
                    userId,
                    username,
                    roles != null ? roles.toArray(new String[0]) : new String[0],
                    jwtConfig.getSecretKey(),
                    jwtConfig.getIssuer(),
                    jwtConfig.getAudience(),
                    jwtConfig.getRefreshExpiresInMinutes()
            );
            
            // 构建响应对象
            AuthLoginResponse response = new AuthLoginResponse();
            response.setToken(newAccessToken);
            response.setRefreshToken(newRefreshToken);
            
            return response;
        } catch (Exception e) {
            // 任何异常都返回 null 表示刷新失败
            return null;
        }
    }
    
    //#endregion
}
