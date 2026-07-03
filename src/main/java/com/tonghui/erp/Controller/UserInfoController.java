package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.Auth.UserInfoDto;
import com.tonghui.erp.Data.Entity.*;
import com.tonghui.erp.Service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 当前用户信息接口
 *
 * 接口清单：
 * ┌────┬──────────────────────────────────────┬────────┬─────────────────────────────────────┐
 * │ #  │ 接口                                 │ 方法   │ 说明                                │
 * ├────┼──────────────────────────────────────┼────────┼─────────────────────────────────────┤
 * │ 1  │ /api/user/info                       │ GET   │ 获取当前登录用户信息                │
 * └────┴──────────────────────────────────────┴────────┴─────────────────────────────────────┘
 *
 * 说明：
 * - 根据JWT令牌获取当前登录用户的详细信息
 * - 包含用户ID、用户名、角色列表、权限按钮标识等
 * - 需要JWT令牌认证
 */
@RestController
@RequestMapping("/api/user")
public class UserInfoController {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    private final UserService userService;
    private final UserRoleService userRoleService;
    private final RoleService roleService;
    private final RolePermService rolePermService;
    private final PermissionService permissionService;

    @Autowired
    public UserInfoController(UserService userService,
                              UserRoleService userRoleService,
                              RoleService roleService,
                              RolePermService rolePermService,
                              PermissionService permissionService) {
        this.userService = userService;
        this.userRoleService = userRoleService;
        this.roleService = roleService;
        this.rolePermService = rolePermService;
        this.permissionService = permissionService;
    }

    // endregion

    // region 用户信息查询接口
    // ===================================
    // 用户信息查询接口
    // ===================================

    /**
     * 获取当前登录用户信息
     *
     * 根据JWT令牌中的用户ID，获取用户的详细信息，包括：
     * - 用户ID和用户名
     * - 角色列表（以roleName作为code返回）
     * - 权限按钮标识（返回permKey列表）
     *
     * 示例请求：
     * GET /api/user/info
     * Headers:
     *   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     *
     * @param request HTTP请求对象，用于获取认证后的用户信息
     * @return 用户信息DTO，包含userId、userName、roles、buttons等字段
     */
    @GetMapping("/info")
    public ApiResponse<UserInfoDto> getCurrentUserInfo(HttpServletRequest request) {
        try {
            Object userIdAttr = request.getAttribute("userId");
            Object usernameAttr = request.getAttribute("username");

            if (userIdAttr == null || usernameAttr == null) {
                return ApiResponse.errorResponse("未认证或令牌无效");
            }

            Long userId = Long.parseLong(userIdAttr.toString());
            String username = usernameAttr.toString();

            // 用户存在性校验
            User user = userService.getById(userId);
            if (user == null) {
                return ApiResponse.errorResponse("用户不存在");
            }

            // 角色（以 roleName 作为 code 返回）
            List<UserRole> userRoles = userRoleService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserRole>().eq("user_id", userId));
            List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
            List<Role> roles = roleIds.isEmpty() ? Collections.emptyList() : roleService.listByIds(roleIds);
            List<String> roleNames = roles.stream().map(Role::getRoleName).filter(Objects::nonNull).collect(Collectors.toList());

            // 权限按钮标识（返回 permKey 列表）
            List<RolePerm> rolePerms = roleIds.isEmpty() ? Collections.emptyList() : rolePermService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RolePerm>().in("role_id", roleIds));
            Set<Long> permIds = rolePerms.stream().map(RolePerm::getPermId).collect(Collectors.toSet());
            List<Permission> permissions = permIds.isEmpty() ? Collections.emptyList() : permissionService.listByIds(permIds);
            List<String> buttonKeys = permissions.stream()
                    .map(Permission::getPermKey)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            UserInfoDto dto = new UserInfoDto();
            dto.setUserId(String.valueOf(userId));
            dto.setUserName(username);
            dto.setRoles(roleNames);
            dto.setButtons(buttonKeys);
            // 系统暂未存储 email 与 avatar 字段，返回空
            dto.setEmail("");
            dto.setAvatar("");

            return ApiResponse.successResponse(dto, "查询成功");
        } catch (Exception ex) {
            return ApiResponse.errorResponse("查询用户信息失败: " + ex.getMessage());
        }
    }

    // endregion
}