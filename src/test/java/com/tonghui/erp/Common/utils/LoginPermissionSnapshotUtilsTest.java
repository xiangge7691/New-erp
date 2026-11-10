package com.tonghui.erp.Common.utils;

import com.tonghui.erp.Common.Dto.System.PermissionDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 登录权限快照工具类单元测试
 * <p>
 * 覆盖权限树拍平、菜单/接口分类、重复节点去重及字符串拼接拆分
 * </p>
 */
public class LoginPermissionSnapshotUtilsTest {

    // region 权限树拍平
    // ===================================
    // 权限树拍平
    // ===================================

    /**
     * 菜单与接口权限按类型分类，并按树顺序拍平
     */
    @Test
    public void testFlattenTree() {
        PermissionDto userMenu = permission(1L, "user_management", "用户管理", "menu", null);
        PermissionDto roleMenu = permission(4L, "role_management", "角色管理", "menu", null);
        PermissionDto createApi = permission(2L, "user:create", "新增用户", "api", userMenu);
        PermissionDto updateApi = permission(3L, "user:update", "修改用户", "api", userMenu);
        // 重复节点用于验证去重
        PermissionDto duplicate = permission(2L, "user:create", "新增用户", "api", userMenu);

        LoginPermissionSnapshotUtils.Snapshot snapshot =
                LoginPermissionSnapshotUtils.flatten(List.of(userMenu, roleMenu, duplicate));

        assertEquals(List.of("user_management", "role_management"), snapshot.getMenuKeys());
        assertEquals(List.of("用户管理", "角色管理"), snapshot.getMenuNames());
        assertEquals(List.of("user:create", "user:update"), snapshot.getPermissionKeys());
        assertEquals(List.of("新增用户", "修改用户"), snapshot.getPermissionNames());
    }

    /**
     * 权限类型为空时按接口权限处理
     */
    @Test
    public void testFlattenNullType() {
        PermissionDto node = permission(9L, "system:config", "系统配置", null, null);

        LoginPermissionSnapshotUtils.Snapshot snapshot =
                LoginPermissionSnapshotUtils.flatten(List.of(node));

        assertTrue(snapshot.getMenuKeys().isEmpty());
        assertEquals(List.of("system:config"), snapshot.getPermissionKeys());
    }

    // endregion

    // region 拼接与拆分
    // ===================================
    // 拼接与拆分
    // ===================================

    /**
     * 权限键使用逗号拼接，名称使用顿号拼接，并可反向拆分
     */
    @Test
    public void testJoinAndSplit() {
        assertEquals("user:create,user:update",
                LoginPermissionSnapshotUtils.joinKeys(List.of("user:create", "user:update")));
        assertEquals("新增用户、修改用户",
                LoginPermissionSnapshotUtils.joinNames(List.of("新增用户", "修改用户")));
        assertEquals(List.of("user:create", "user:update"),
                LoginPermissionSnapshotUtils.splitKeys("user:create,user:update"));
        assertEquals(List.of("新增用户", "修改用户"),
                LoginPermissionSnapshotUtils.splitNames("新增用户、修改用户"));
        assertEquals(List.of(1L, 25L), LoginPermissionSnapshotUtils.splitIds("1,25"));
        assertTrue(LoginPermissionSnapshotUtils.splitIds("1,abc,25").size() == 2);
        assertNull(LoginPermissionSnapshotUtils.joinKeys(new ArrayList<>()));
    }

    // endregion

    // region 测试辅助
    // ===================================
    // 测试辅助
    // ===================================

    /**
     * 构造权限节点
     *
     * @param id       权限ID
     * @param permKey  权限键
     * @param permName 权限名称
     * @param permType 权限类型
     * @param parent   父节点（挂载到其 children）
     * @return 权限节点
     */
    private PermissionDto permission(Long id, String permKey, String permName,
                                     String permType, PermissionDto parent) {
        PermissionDto dto = new PermissionDto();
        dto.setId(id);
        dto.setPermKey(permKey);
        dto.setPermName(permName);
        dto.setPermType(permType);
        if (parent != null) {
            parent.getChildren().add(dto);
        }
        return dto;
    }

    // endregion
}
