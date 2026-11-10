package com.tonghui.erp.Controller;

import com.tonghui.erp.Common.Dto.Auth.LoginRequest;
import com.tonghui.erp.Common.Dto.Auth.LoginResponse;
import com.tonghui.erp.Common.Dto.Log.LoginLogContext;
import com.tonghui.erp.Common.Dto.System.PermissionDto;
import com.tonghui.erp.Common.Dto.System.RoleDto;
import com.tonghui.erp.Common.Dto.System.UserDto;
import com.tonghui.erp.Service.LoginLogService;
import com.tonghui.erp.Service.LoginService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录日志角色/权限快照单元测试
 * <p>
 * 使用 Mock 登录服务验证 AuthController 会把角色列表、菜单范围和接口权限
 * 写入登录日志上下文，不依赖数据库
 * </p>
 */
public class AuthControllerLoginLogTest {

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 登录成功时记录角色、菜单与接口权限快照
     */
    @Test
    public void testLoginRecordsAccessSnapshot() {
        LoginService loginService = mock(LoginService.class);
        LoginLogService loginLogService = mock(LoginLogService.class);
        AuthController controller = new AuthController(loginService, loginLogService);

        LoginResponse response = successResponse();
        when(loginService.loginWithFullResponseAsync("root", "root"))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(loginLogService.buildContext(any(), eq("root"), any()))
                .thenReturn(new LoginLogContext());

        LoginRequest request = new LoginRequest();
        request.setUserName("root");
        request.setPassword("root");
        controller.login(request, new MockHttpServletRequest());

        ArgumentCaptor<LoginLogContext> captor = ArgumentCaptor.forClass(LoginLogContext.class);
        verify(loginLogService).recordAsync(captor.capture());
        LoginLogContext context = captor.getValue();

        assertEquals(List.of(1L, 25L), context.getRoleIds());
        assertEquals(List.of("超级管理员", "系统管理员"), context.getRoleNames());
        assertEquals(List.of("user_management", "role_management"), context.getMenuKeys());
        assertEquals(List.of("用户管理", "角色管理"), context.getMenuNames());
        assertEquals(List.of("user:create", "user:update"), context.getPermissionKeys());
        assertEquals(List.of("新增用户", "修改用户"), context.getPermissionNames());
    }

    // endregion

    // region 测试辅助
    // ===================================
    // 测试辅助
    // ===================================

    /**
     * 构造登录成功响应（含角色与权限树）
     *
     * @return 登录响应
     */
    private LoginResponse successResponse() {
        UserDto user = new UserDto();
        user.setId(1L);
        user.setUserName("root");
        user.setName("超级管理员");

        RoleDto superAdmin = new RoleDto();
        superAdmin.setRoleId(1L);
        superAdmin.setRoleName("超级管理员");
        RoleDto sysAdmin = new RoleDto();
        sysAdmin.setRoleId(25L);
        sysAdmin.setRoleName("系统管理员");

        PermissionDto userMenu = permission(1L, "user_management", "用户管理", "menu");
        PermissionDto roleMenu = permission(2L, "role_management", "角色管理", "menu");
        userMenu.getChildren().add(permission(3L, "user:create", "新增用户", "api"));
        userMenu.getChildren().add(permission(4L, "user:update", "修改用户", "api"));

        LoginResponse.LoginData data = new LoginResponse.LoginData();
        data.setToken("token");
        data.setRefreshToken("refresh-token");
        data.setUser(user);
        data.setRoles(List.of(superAdmin, sysAdmin));
        data.setPermissionTree(List.of(userMenu, roleMenu));

        LoginResponse response = new LoginResponse();
        response.setSuccess(true);
        response.setCode(200);
        response.setMessage("登录成功");
        response.setData(data);
        return response;
    }

    /**
     * 构造权限节点
     *
     * @param id       权限ID
     * @param permKey  权限键
     * @param permName 权限名称
     * @param permType 权限类型
     * @return 权限节点
     */
    private PermissionDto permission(Long id, String permKey, String permName, String permType) {
        PermissionDto dto = new PermissionDto();
        dto.setId(id);
        dto.setPermKey(permKey);
        dto.setPermName(permName);
        dto.setPermType(permType);
        return dto;
    }

    // endregion
}
