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
 * 对齐 openapi: /api/user/info
 */
@RestController
@RequestMapping("/api/user")
public class UserInfoController {

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
}