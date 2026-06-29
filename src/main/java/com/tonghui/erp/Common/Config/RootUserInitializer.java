package com.tonghui.erp.Common.Config;

import com.tonghui.erp.Common.utils.PasswordHasher;
import com.tonghui.erp.Data.Entity.*;
import com.tonghui.erp.Service.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统初始化类
 * <p>
 * 在系统启动时自动检测是否存在root用户和角色，
 * 如果不存在则自动创建包含所有权限的root用户和角色
 * </p>
 */
@Component
public class RootUserInitializer {

    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final UserRoleService userRoleService;
    private final RolePermService rolePermService;

    @Autowired
    public RootUserInitializer(@Lazy UserService userService, 
                              @Lazy RoleService roleService, 
                              @Lazy PermissionService permissionService, 
                              @Lazy UserRoleService userRoleService, 
                              @Lazy RolePermService rolePermService) {
        this.userService = userService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.userRoleService = userRoleService;
        this.rolePermService = rolePermService;
    }

    /**
     * 系统启动后自动执行初始化逻辑
     */
    @PostConstruct
    public void initializeRootUserAndRole() {
        try {
            // 检查并创建root角色
            Role rootRole = ensureRootRoleExists();

            // 检查并创建root用户
            User rootUser = ensureRootUserExists();

            // 绑定root用户和root角色
            ensureRootUserRoleBinding(rootUser, rootRole);

            // 确保root角色拥有所有权限
            ensureRootRoleHasAllPermissions(rootRole);
        } catch (Exception e) {
            // 记录错误但不中断应用启动
            System.err.println("初始化root用户和角色时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 确保root角色存在
     *
     * @return root角色对象
     */
    private Role ensureRootRoleExists() {
        // 查询是否已存在root角色
        Role role = roleService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Role>()
                        .eq("role_name", "root")
        );

        // 如果不存在，则创建
        if (role == null) {
            role = new Role();
            role.setRoleName("root");
            role.setRoleDesc("超级管理员角色，拥有系统所有权限");
            role.setRoleStatus(1); // 启用状态
            role.setCreateTime(LocalDateTime.now());
            role.setUpdateTime(LocalDateTime.now());
            roleService.save(role);
        }

        return role;
    }

    /**
     * 确保root用户存在
     *
     * @return root用户对象
     */
    private User ensureRootUserExists() {
        // 查询是否已存在root用户
        User user = userService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("user_account", "root")
        );

        // 如果不存在，则创建
        if (user == null) {
            user = new User();
            user.setUserAccount("root");
            user.setUserName("超级管理员");
            // 密码也设置为root
            user.setPassword(PasswordHasher.hashPassword("root"));
            user.setPhone("");
            user.setGender(1); // 使用数字1表示男性，避免字符串长度问题
            user.setUserStatus(1); // 启用状态
            user.setCreatedTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userService.save(user);
        }

        return user;
    }

    /**
     * 确保root用户与root角色绑定
     *
     * @param user root用户
     * @param role root角色
     */
    private void ensureRootUserRoleBinding(User user, Role role) {
        // 查询是否已存在绑定关系
        UserRole userRole = userRoleService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserRole>()
                        .eq("user_id", user.getUserId())
                        .eq("role_id", role.getRoleId())
        );

        // 如果不存在，则创建绑定关系
        if (userRole == null) {
            userRole = new UserRole();
            userRole.setUserId(user.getUserId());
            userRole.setRoleId(role.getRoleId());
            userRole.setCreatedAt(LocalDateTime.now());
            userRoleService.save(userRole);
        }
    }

    /**
     * 确保root角色拥有所有权限
     *
     * @param role root角色
     */
    private void ensureRootRoleHasAllPermissions(Role role) {
        // 获取所有权限
        List<Permission> allPermissions = permissionService.list();

        // 获取当前角色已有的权限
        List<RolePerm> existingRolePerms = rolePermService.list(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RolePerm>()
                        .eq("role_id", role.getRoleId())
        );

        // 获取已存在的权限ID列表
        List<Long> existingPermIds = existingRolePerms.stream()
                .map(RolePerm::getPermId)
                .collect(Collectors.toList());

        // 为每个权限检查是否已分配给root角色，如果没有则分配
        for (Permission permission : allPermissions) {
            if (!existingPermIds.contains(permission.getPermId())) {
                RolePerm rolePerm = new RolePerm();
                rolePerm.setRoleId(role.getRoleId());
                rolePerm.setPermId(permission.getPermId());
                rolePerm.setCreatedAt(LocalDateTime.now());
                rolePermService.save(rolePerm);
            }
        }
    }
}
