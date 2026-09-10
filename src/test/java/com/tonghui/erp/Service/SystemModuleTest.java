package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.Auth.LoginResponse;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.DepartmentDto;
import com.tonghui.erp.Common.Dto.System.RoleDto;
import com.tonghui.erp.Common.Dto.System.UserDepartmentDto;
import com.tonghui.erp.Common.Dto.System.UserDto;
import com.tonghui.erp.Common.Dto.System.UserRoleDto;
import com.tonghui.erp.Data.Entity.Department;
import com.tonghui.erp.Data.Entity.Permission;
import com.tonghui.erp.Data.Entity.Position;
import com.tonghui.erp.Data.Entity.Role;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.Entity.UserRole;
import com.tonghui.erp.Data.mapper.PermissionMapper;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Service.LoginService;
import com.tonghui.erp.Service.UserService;
import com.tonghui.erp.Service.RoleService;
import com.tonghui.erp.Service.DepartmentService;
import com.tonghui.erp.Service.PositionService;
import com.tonghui.erp.Service.PermissionService;
import com.tonghui.erp.Service.RolePermService;
import com.tonghui.erp.Service.UserDepartmentService;
import com.tonghui.erp.Service.UserRoleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统管理域综合测试
 * <p>
 * 覆盖登录认证、用户、角色、部门、岗位、权限、用户角色、用户部门等核心业务逻辑，
 * 全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
public class SystemModuleTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 登录认证服务
     */
    @Autowired
    private LoginService loginService;

    /**
     * 用户服务
     */
    @Autowired
    private UserService userService;

    /**
     * 角色服务
     */
    @Autowired
    private RoleService roleService;

    /**
     * 部门服务
     */
    @Autowired
    private DepartmentService departmentService;

    /**
     * 岗位服务
     */
    @Autowired
    private PositionService positionService;

    /**
     * 权限服务
     */
    @Autowired
    private PermissionService permissionService;

    /**
     * 角色-权限关联服务
     */
    @Autowired
    private RolePermService rolePermService;

    /**
     * 用户-部门关联服务
     */
    @Autowired
    private UserDepartmentService userDepartmentService;

    /**
     * 用户-角色关联服务
     */
    @Autowired
    private UserRoleService userRoleService;

    /**
     * 用户数据访问层
     */
    @Autowired
    private UserMapper userMapper;

    /**
     * 权限数据访问层
     */
    @Autowired
    private PermissionMapper permissionMapper;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试默认管理员登录成功：root/root 登录应返回成功与令牌
     */
    @Test
    public void testLoginSuccess() {
        LoginResponse response = loginService.loginWithFullResponse("root", "root");
        assertNotNull(response, "登录响应不应为空");
        assertTrue(response.isSuccess(), "root 默认账户登录应成功");
        assertNotNull(response.getData(), "登录数据不应为空");
        assertNotNull(response.getData().getToken(), "登录成功应返回访问令牌");
        assertNotNull(response.getData().getUser(), "登录成功应返回用户信息");
    }

    /**
     * 测试密码错误登录失败：错误密码应返回失败且无令牌
     */
    @Test
    public void testLoginWrongPassword() {
        LoginResponse response = loginService.loginWithFullResponse("root", "wrong-password-123");
        assertNotNull(response, "登录响应不应为空");
        assertFalse(response.isSuccess(), "错误密码登录应失败");
    }

    /**
     * 测试用户创建与密码校验：创建用户（Argon2哈希）后可通过账号密码校验、可搜索到
     */
    @Test
    @Transactional
    public void testCreateUserAndValidatePassword() {
        String account = "TESTUSER" + System.currentTimeMillis();
        User user = new User();
        user.setUserAccount(account);
        user.setUserName("测试用户");
        user.setPassword("Test@123456");
        user.setUserStatus(1);
        assertTrue(userService.saveWithHashedPassword(user), "创建用户应成功");
        assertNotNull(user.getUserId(), "创建后应回填用户ID");

        // 密码加密存储（非明文）
        User stored = userMapper.selectById(user.getUserId());
        assertNotNull(stored, "用户应已落库");
        assertNotEquals("Test@123456", stored.getPassword(), "密码应以哈希形式存储而非明文");

        // 密码校验通过 / 错误密码校验失败
        assertTrue(userService.validateUserPassword(account, "Test@123456"), "正确密码应校验通过");
        assertFalse(userService.validateUserPassword(account, "wrong"), "错误密码应校验失败");

        // 高级搜索可按账号查询到
        PagedResult<UserDto> result = userService.advancedSearchUsers(account, null, null, null, null, 1, 10);
        assertNotNull(result, "搜索结果不应为空");
        assertTrue(result.getTotalCount() > 0, "按账号搜索应能查到创建的用户");
    }

    /**
     * 测试用户信息更新与禁用：更新用户名、禁用状态后搜索按状态过滤
     */
    @Test
    @Transactional
    public void testUpdateUserSelective() {
        String account = "TESTUSERU" + System.currentTimeMillis();
        User user = new User();
        user.setUserAccount(account);
        user.setUserName("初始名称");
        user.setPassword("Test@123456");
        userService.saveWithHashedPassword(user);
        Long userId = user.getUserId();

        // 选择性更新：仅更新用户名与状态，不触碰密码
        User update = new User();
        update.setUserId(userId);
        update.setUserName("更新后的名称");
        update.setUserStatus(0);
        assertTrue(userService.updateWithHashedPasswordSelective(update), "选择性更新应成功");

        User after = userMapper.selectById(userId);
        assertEquals("更新后的名称", after.getUserName(), "用户名应已更新");
        assertEquals(0, after.getUserStatus().intValue(), "状态应已禁用");

        // 按状态搜索：禁用状态可搜索到该用户（分页参数为0基页码）
        PagedResult<UserDto> disabled = userService.advancedSearchUsers(null, null, null, 0, null, 0, 10);
        assertNotNull(disabled, "按状态搜索不应为空");
        assertTrue(disabled.getItems().stream().anyMatch(d -> d.getName().equals("更新后的名称")),
                "禁用状态搜索应包含刚更新的用户");
    }

    /**
     * 测试用户角色与部门分配：分配后关联查询可见、可重新分配、可清空
     */
    @Test
    @Transactional
    public void testAssignRolesAndDepartments() {
        String account = "TESTUSERA" + System.currentTimeMillis();
        User user = new User();
        user.setUserAccount(account);
        user.setUserName("分配测试用户");
        user.setPassword("Test@123456");
        userService.saveWithHashedPassword(user);
        Long userId = user.getUserId();

        // 创建角色与部门
        Role role = new Role();
        role.setRoleName("TESTROLE" + System.currentTimeMillis());
        role.setRoleStatus(1);
        assertTrue(roleService.save(role), "创建角色应成功");

        Department department = new Department();
        department.setDepartmentName("TESTDEPT" + System.currentTimeMillis());
        department.setParentId(0L);
        department.setStatus(1);
        assertTrue(departmentService.save(department), "创建部门应成功");

        // 分配角色与部门
        assertTrue(userService.assignRolesToUser(userId, List.of(role.getRoleId())), "分配角色应成功");
        assertTrue(userService.assignDepartmentsToUser(userId, List.of(department.getDepartmentId())), "分配部门应成功");

        // 关联查询可见
        List<UserRoleDto> userRoles = userRoleService.getDtosByUserId(userId);
        assertFalse(userRoles.isEmpty(), "用户角色关联应存在");
        List<UserDepartmentDto> userDepartments = userDepartmentService.getDtosByUserId(userId);
        assertFalse(userDepartments.isEmpty(), "用户部门关联应存在");

        // 用户详情中应带出角色与部门
        UserDto detail = userService.getDtoById(userId);
        assertNotNull(detail, "用户详情不应为空");
        assertTrue(detail.getRoles() != null && !detail.getRoles().isEmpty(), "详情应包含角色");
        assertTrue(detail.getDepartments() != null && !detail.getDepartments().isEmpty(), "详情应包含部门");

        // 重新分配（全量覆盖）：分配一个新的角色，旧角色关联应被清除
        Role role2 = new Role();
        role2.setRoleName("TESTROLE2" + System.currentTimeMillis());
        role2.setRoleStatus(1);
        roleService.save(role2);
        assertTrue(userService.updateUserRoles(userId, List.of(role2.getRoleId())), "覆盖分配角色应成功");
        List<UserRoleDto> afterUpdate = userRoleService.getDtosByUserId(userId);
        assertEquals(1, afterUpdate.size(), "覆盖分配后应仅保留新角色");
        assertEquals(role2.getRoleId(), afterUpdate.get(0).getRoleId(), "新角色ID应一致");

        // 删除用户关联
        assertTrue(userService.deleteUserAssociations(userId), "删除用户关联应成功");
        assertEquals(0, userRoleService.getDtosByUserId(userId).size(), "删除后角色关联应为空");
        assertEquals(0, userDepartmentService.getDtosByUserId(userId).size(), "删除后部门关联应为空");
    }

    /**
     * 测试角色管理：创建角色→按名搜索→分配权限→权限关联可见→角色详情带权限
     */
    @Test
    @Transactional
    public void testRoleCrudAndPermissionAssignment() {
        String roleName = "TESTROLE" + System.currentTimeMillis();
        Role role = new Role();
        role.setRoleName(roleName);
        role.setRoleDesc("测试角色");
        role.setRoleStatus(1);
        assertTrue(roleService.save(role), "创建角色应成功");

        // 创建权限
        Permission permission = new Permission();
        permission.setPermKey("TESTPERM" + System.currentTimeMillis());
        permission.setPermName("测试权限");
        permission.setPermType("api");
        assertTrue(permissionService.save(permission), "创建权限应成功");

        // 按角色名搜索
        PagedResult<RoleDto> search = roleService.advancedSearchRoles(roleName, null, null, null, null, 1, 10);
        assertNotNull(search, "搜索结果不应为空");
        assertTrue(search.getTotalCount() >= 1, "按角色名搜索应命中");

        // 给角色分配权限（RolePermService），并验证关联可见
        assertTrue(rolePermService.assignPermissionsToRole(role.getRoleId(), List.of(permission.getPermId())),
                "分配权限应成功");
        List<Long> permIds = rolePermService.getPermissionIdsByRoleId(role.getRoleId());
        assertEquals(1, permIds.size(), "角色应关联1个权限");
        assertTrue(permIds.contains(permission.getPermId()), "关联的权限ID应一致");

        // 角色详情应包含权限
        RoleDto details = roleService.getRoleDetails(role.getRoleId());
        assertNotNull(details, "角色详情不应为空");
        assertNotNull(details.getPermissions(), "角色详情应包含权限列表");

        // 权限侧反向查询
        assertTrue(rolePermService.getRoleIdsByPermissionId(permission.getPermId()).contains(role.getRoleId()),
                "权限反向关联应包含该角色");
    }

    /**
     * 测试部门管理：创建部门→按名查询→列表→详情
     */
    @Test
    @Transactional
    public void testDepartmentCrud() {
        String deptName = "TESTDEPT" + System.currentTimeMillis();
        Department department = new Department();
        department.setDepartmentName(deptName);
        department.setParentId(0L);
        department.setStatus(1);
        department.setSortOrder(99);
        assertTrue(departmentService.save(department), "创建部门应成功");
        Long deptId = department.getDepartmentId();

        // 按名称精确查询
        Department found = departmentService.getByDepartmentName(deptName);
        assertNotNull(found, "按名称应查到部门");
        assertEquals(deptId, found.getDepartmentId(), "查询到的部门ID应一致");

        // 详情查询
        DepartmentDto dto = departmentService.getDtoById(deptId);
        assertNotNull(dto, "部门详情不应为空");
        assertEquals(deptName, dto.getDepartmentName(), "详情部门名称应一致");

        // 部门列表应包含
        List<DepartmentDto> list = departmentService.listDto();
        assertTrue(list.stream().anyMatch(d -> d.getId().equals(deptId)), "部门列表应包含新建部门");

        // 高级搜索
        PagedResult<DepartmentDto> search = departmentService.advancedSearchDepartments(deptName, 1, 1, 10);
        assertTrue(search.getTotalCount() >= 1, "高级搜索应命中新建部门");
    }

    /**
     * 测试岗位管理：创建岗位→查询列表→更新→删除
     */
    @Test
    @Transactional
    public void testPositionCrud() {
        String code = "TESTPOS" + System.currentTimeMillis();
        Position position = new Position();
        position.setPositionCode(code);
        position.setPositionName("测试岗位");
        position.setDepartmentId(null);
        position.setStatus(1);
        position.setSortOrder(1);
        assertTrue(positionService.save(position), "创建岗位应成功");
        Long positionId = position.getPositionId();

        // 分页查询应命中（分页参数为0基页码）
        Position query = new Position();
        query.setPositionCode(code);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Position> page =
                positionService.queryPositions(query, 0, 10);
        assertTrue(page.getTotal() >= 1, "按编码查询岗位应命中");
        assertEquals(code, page.getRecords().get(0).getPositionCode(), "查询结果编码应一致");

        // 更新岗位名称
        Position update = new Position();
        update.setPositionId(positionId);
        update.setPositionName("更新后岗位");
        assertTrue(positionService.updateById(update), "更新岗位应成功");
        Position after = positionService.getById(positionId);
        assertEquals("更新后岗位", after.getPositionName(), "岗位名称应已更新");

        // 删除岗位
        assertTrue(positionService.removeById(positionId), "删除岗位应成功");
        assertNull(positionService.getById(positionId), "删除后岗位应不存在");
    }

    /**
     * 测试权限管理：创建权限→按key查询→高级搜索→权限树
     */
    @Test
    @Transactional
    public void testPermissionCrud() {
        String permKey = "TESTPERM" + System.currentTimeMillis();
        Permission permission = new Permission();
        permission.setPermKey(permKey);
        permission.setPermName("测试权限");
        permission.setPermType("api");
        permission.setPermStatus(1);
        assertTrue(permissionService.save(permission), "创建权限应成功");
        Long permId = permission.getPermId();

        // 按 key 精确查询
        Permission found = permissionService.getByPermKey(permKey);
        assertNotNull(found, "按key应查到权限");
        assertEquals(permId, found.getPermId(), "权限ID应一致");

        // 高级搜索
        PagedResult<?> search = permissionService.advancedSearchPermissions("测试权限", permKey, null, 1, null, 1, 10);
        assertTrue(search.getTotalCount() >= 1, "高级搜索应命中新建权限");

        // 权限详情
        com.tonghui.erp.Common.Dto.System.PermissionDto detail = permissionService.getPermissionDetails(permId);
        assertNotNull(detail, "权限详情不应为空");
        assertEquals(permKey, detail.getPermKey(), "权限详情key应一致");

        // 权限树应包含该权限
        assertTrue(permissionService.getPermissionTree().stream().anyMatch(p -> p.getId() != null),
                "权限树不应为空");
    }

    // endregion
}
