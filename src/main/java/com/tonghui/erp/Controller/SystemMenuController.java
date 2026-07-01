package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.ApiResponse;
import com.tonghui.erp.Common.Dto.System.AppRouteRecordDto;
import com.tonghui.erp.Common.Dto.System.PermissionDto;
import com.tonghui.erp.Common.Dto.System.RouteMetaDto;
import com.tonghui.erp.Service.PermissionService;
import com.tonghui.erp.Service.UserService;
import com.tonghui.erp.Common.utils.JwtHelper;
import com.tonghui.erp.Common.Config.JwtConfig;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Service.UserRoleService;
import com.tonghui.erp.Service.RolePermService;
import com.tonghui.erp.Data.Entity.UserRole;
import com.tonghui.erp.Data.Entity.RolePerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * 系统菜单接口（简化）
 * 对齐 openapi: /api/system/menus/simple
 */
@RestController
@RequestMapping("/api/system/menus")
public class SystemMenuController {

    private final PermissionService permissionService;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final RolePermService rolePermService;
    private final JwtConfig jwtConfig;

    @Autowired
    public SystemMenuController(PermissionService permissionService, 
                               UserService userService,
                               UserRoleService userRoleService,
                               RolePermService rolePermService,
                               JwtConfig jwtConfig) {
        this.permissionService = permissionService;
        this.userService = userService;
        this.userRoleService = userRoleService;
        this.rolePermService = rolePermService;
        this.jwtConfig = jwtConfig;
    }

    @GetMapping("/simple")
    public ApiResponse<List<AppRouteRecordDto>> getSimpleMenus(HttpServletRequest request) {
        try {
            // 从请求头中获取token
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ApiResponse.errorResponse("未提供有效的认证令牌");
            }
            
            String token = authHeader.substring(7);
            
            // 从token中解析用户ID
            String userIdStr = JwtHelper.getUserIdFromToken(token, jwtConfig.getSecretKey());
            if (userIdStr == null || userIdStr.isEmpty()) {
                return ApiResponse.errorResponse("无效的认证令牌");
            }
            
            Long userId = Long.parseLong(userIdStr);
            
            // 获取用户拥有的权限ID集合
            Set<Long> userPermissionIds = getUserPermissionIds(userId);
            
            // 获取完整权限树
            List<PermissionDto> tree = permissionService.getPermissionTree();

            // 过滤启用状态、类型为菜单（若类型为空则默认视为菜单）并且用户有权限访问的节点
            List<PermissionDto> filtered = filterPermissionsByUserAccess(tree, userPermissionIds);

            // 转换为路由记录
            List<AppRouteRecordDto> routes = filtered.stream()
                    .map(this::toRouteRecord)
                    .collect(Collectors.toList());

            return ApiResponse.successResponse(routes, "查询成功");
        } catch (Exception ex) {
            return ApiResponse.errorResponse("获取菜单失败: " + ex.getMessage());
        }
    }

    /**
     * 获取用户拥有的权限ID集合
     * @param userId 用户ID
     * @return 权限ID集合
     */
    private Set<Long> getUserPermissionIds(Long userId) {
        Set<Long> permissionIds = new HashSet<>();
        
        // 获取用户的角色
        List<UserRole> userRoles = userRoleService.list(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserRole>()
                .eq("user_id", userId)
        );
        
        // 获取角色对应的权限
        for (UserRole userRole : userRoles) {
            List<RolePerm> rolePerms = rolePermService.list(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RolePerm>()
                    .eq("role_id", userRole.getRoleId())
            );
            
            for (RolePerm rolePerm : rolePerms) {
                permissionIds.add(rolePerm.getPermId());
            }
        }
        
        return permissionIds;
    }

    /**
     * 根据用户权限过滤权限树
     * @param permissions 权限列表
     * @param userPermissionIds 用户拥有的权限ID集合
     * @return 过滤后的权限列表
     */
    private List<PermissionDto> filterPermissionsByUserAccess(List<PermissionDto> permissions, Set<Long> userPermissionIds) {
        List<PermissionDto> result = new ArrayList<>();
        
        for (PermissionDto permission : permissions) {
            // 检查权限是否启用
            if (permission.getStatus() != null && permission.getStatus() != 1) {
                continue;
            }
            
            // 检查权限类型是否为菜单
            if (permission.getPermType() != null && !"menu".equalsIgnoreCase(permission.getPermType())) {
                continue;
            }
            
            // 检查用户是否有此权限
            if (!userPermissionIds.contains(permission.getId())) {
                // 即使用户没有此权限，但如果它有子权限，我们仍需要保留它以构建菜单结构
                PermissionDto filteredPermission = filterPermissionWithChildren(permission, userPermissionIds);
                if (filteredPermission != null) {
                    result.add(filteredPermission);
                }
                continue;
            }
            
            // 用户有权限，添加到结果中（同时过滤子权限）
            PermissionDto permissionCopy = copyPermissionWithoutChildren(permission);
            permissionCopy.setChildren(filterPermissionsByUserAccess(permission.getChildren(), userPermissionIds));
            result.add(permissionCopy);
        }
        
        return result;
    }
    
    /**
     * 过滤权限及其子权限
     * @param permission 权限
     * @param userPermissionIds 用户拥有的权限ID集合
     * @return 过滤后的权限，如果没有子权限则返回null
     */
    private PermissionDto filterPermissionWithChildren(PermissionDto permission, Set<Long> userPermissionIds) {
        PermissionDto permissionCopy = copyPermissionWithoutChildren(permission);
        List<PermissionDto> filteredChildren = filterPermissionsByUserAccess(permission.getChildren(), userPermissionIds);
        
        if (!filteredChildren.isEmpty()) {
            permissionCopy.setChildren(filteredChildren);
            return permissionCopy;
        }
        
        // 如果没有子权限且用户没有此权限，则不返回此节点
        return null;
    }
    
    /**
     * 复制权限对象但不复制其子权限
     * @param source 源权限对象
     * @return 复制的权限对象
     */
    private PermissionDto copyPermissionWithoutChildren(PermissionDto source) {
        PermissionDto copy = new PermissionDto();
        copy.setId(source.getId());
        copy.setPermKey(source.getPermKey());
        copy.setPermName(source.getPermName());
        copy.setPermType(source.getPermType());
        copy.setParentId(source.getParentId());
        copy.setDisplayOrder(source.getDisplayOrder());
        copy.setStatus(source.getStatus());
        copy.setCreatedTime(source.getCreatedTime());
        copy.setUpdatedTime(source.getUpdatedTime());
        return copy;
    }

    private String getFirstLeafPath(List<AppRouteRecordDto> routes) {
        if (routes == null || routes.isEmpty()) return null;
        for (AppRouteRecordDto r : routes) {
            if (r.getChildren() == null || r.getChildren().isEmpty()) {
                return r.getPath();
            }
            String child = getFirstLeafPath(r.getChildren());
            if (child != null) return child;
        }
        return null;
    }

    private AppRouteRecordDto toRouteRecord(PermissionDto p) {
        AppRouteRecordDto route = new AppRouteRecordDto();
        String path = p.getPermKey() != null ? "/" + p.getPermKey() : "/" + (p.getId() != null ? p.getId() : "route");
        route.setPath(path);
        route.setName(p.getPermKey() != null ? p.getPermKey() : p.getPermName());
        route.setComponent("");

        RouteMetaDto meta = new RouteMetaDto();
        meta.setTitle(p.getPermName());
        meta.setI18n(false);
        route.setMeta(meta);

        // 处理子节点：同样过滤启用状态与菜单类型
        List<AppRouteRecordDto> children = p.getChildren().stream()
                .filter(c -> c.getStatus() == null || Objects.equals(c.getStatus(), 1))
                .filter(c -> c.getPermType() == null || "menu".equalsIgnoreCase(c.getPermType()))
                .map(this::toRouteRecord)
                .collect(Collectors.toList());
        route.setChildren(children);
        return route;
    }
}