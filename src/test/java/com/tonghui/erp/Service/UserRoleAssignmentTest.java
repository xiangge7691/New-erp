package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tonghui.erp.Common.Dto.System.UserDepartmentDto;
import com.tonghui.erp.Common.Dto.System.UserRoleDto;
import com.tonghui.erp.Data.Entity.Department;
import com.tonghui.erp.Data.Entity.Role;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.Entity.UserRole;
import com.tonghui.erp.Data.mapper.UserRoleMapper;
import com.tonghui.erp.Service.DepartmentService;
import com.tonghui.erp.Service.RoleService;
import com.tonghui.erp.Service.UserDepartmentService;
import com.tonghui.erp.Service.UserRoleService;
import com.tonghui.erp.Service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户角色/部门分配专项测试
 * <p>
 * 覆盖"先删后插"分配逻辑在软删除 + 唯一键场景下的正确性：
 * 重复分配相同角色/部门不得因软删残留占用唯一键而失败（回归 bug：PUT /api/User/{id} 权限赋不上），
 * 空列表应清空关联，全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
public class UserRoleAssignmentTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 用户服务，用于角色/部门分配
     */
    @Autowired
    private UserService userService;

    /**
     * 角色服务，用于创建测试角色
     */
    @Autowired
    private RoleService roleService;

    /**
     * 部门服务，用于创建测试部门
     */
    @Autowired
    private DepartmentService departmentService;

    /**
     * 用户角色关联查询服务
     */
    @Autowired
    private UserRoleService userRoleService;

    /**
     * 用户部门关联查询服务
     */
    @Autowired
    private UserDepartmentService userDepartmentService;

    /**
     * 用户角色关联 Mapper（用于构造软删残留现场）
     */
    @Autowired
    private UserRoleMapper userRoleMapper;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试重复分配相同角色不因软删残留冲突而失败
     * <p>
     * 回归场景：第一次分配后再次分配同一角色。修复前先软删除再插入，
     * 唯一键 uk_user_role(user_id, role_id) 仍被软删行占用导致 Duplicate entry 返回 false
     * </p>
     */
    @Test
    @Transactional
    public void testReassignSameRoleSucceeds() {
        User user = createTestUser();
        Long userId = user.getUserId();
        Long roleId = createTestRole().getRoleId();

        // 第一次分配
        assertTrue(userService.assignRolesToUser(userId, List.of(roleId)), "第一次分配角色应成功");
        assertEquals(1, userRoleService.getDtosByUserId(userId).size(), "第一次分配后应有1条角色关联");

        // 再次分配同一角色（模拟前端多次保存相同 roleIds）
        assertTrue(userService.assignRolesToUser(userId, List.of(roleId)), "重复分配相同角色应成功");

        // 验证：无软删残留、仅1条可用关联、角色仍正确
        List<UserRoleDto> roles = userRoleService.getDtosByUserId(userId);
        assertEquals(1, roles.size(), "重复分配后应仅保留1条角色关联");
        assertEquals(roleId, roles.get(0).getRoleId(), "角色ID应一致");
        assertEquals(0, userRoleMapper.selectCount(new QueryWrapper<UserRole>()
                        .eq("user_id", userId).eq("is_deleted", 1)).intValue(),
                "不应残留软删记录");
    }

    /**
     * 测试将角色从一组替换为另一组（全量覆盖）时不产生行列错乱
     */
    @Test
    @Transactional
    public void testReplaceRolesWithDifferentSet() {
        User user = createTestUser();
        Long userId = user.getUserId();
        Long roleA = createTestRole().getRoleId();
        Long roleB = createTestRole().getRoleId();

        assertTrue(userService.assignRolesToUser(userId, List.of(roleA)), "分配角色A应成功");
        assertTrue(userService.assignRolesToUser(userId, List.of(roleB)), "更新为角色B应成功");

        List<UserRoleDto> roles = userRoleService.getDtosByUserId(userId);
        assertEquals(1, roles.size(), "全量覆盖后应仅剩1条角色关联");
        assertEquals(roleB, roles.get(0).getRoleId(), "应保留新的角色B");
    }

    /**
     * 测试空角色列表清空关联：分配后传空列表，所有角色关联应被清除（物理删除，无软删残留）
     */
    @Test
    @Transactional
    public void testClearRolesWithEmptyList() {
        User user = createTestUser();
        Long userId = user.getUserId();
        Long roleId = createTestRole().getRoleId();

        assertTrue(userService.assignRolesToUser(userId, List.of(roleId)), "分配角色应成功");
        assertTrue(userService.assignRolesToUser(userId, List.of()), "空列表清空角色应成功");
        assertEquals(0, userRoleService.getDtosByUserId(userId).size(), "清空后不应有角色关联");
        assertEquals(0, userRoleMapper.selectCount(new QueryWrapper<UserRole>()
                        .eq("user_id", userId).eq("is_deleted", 1)).intValue(),
                "清空后不应残留软删记录");
    }

    /**
     * 测试已存在软删残留现场时重新分配仍成功（还原线上 user 35 的故障数据形态）
     * <p>
     * 手工构造 is_deleted=1 的同键记录后重新分配，应物理清掉残留并插入新记录
     * </p>
     */
    @Test
    @Transactional
    public void testReassignRoleWithExistingSoftDeletedRow() {
        User user = createTestUser();
        Long userId = user.getUserId();
        Long roleId = createTestRole().getRoleId();

        // 构造故障现场：一条软删的同键记录
        UserRole leftover = new UserRole();
        leftover.setUserId(userId);
        leftover.setRoleId(roleId);
        leftover.setIsDeleted(1);
        leftover.setCreatedTime(LocalDateTime.now());
        userRoleService.save(leftover);

        assertTrue(userService.assignRolesToUser(userId, List.of(roleId)), "存在软删残留时重新分配应成功");
        List<UserRoleDto> roles = userRoleService.getDtosByUserId(userId);
        assertEquals(1, roles.size(), "应仅保留1条可用角色关联");
        assertEquals(0, userRoleMapper.selectCount(new QueryWrapper<UserRole>()
                        .eq("user_id", userId).eq("is_deleted", 1)).intValue(),
                "软删残留应被物理清除");
    }

    /**
     * 测试重复分配相同部门不因软删残留冲突而失败（user_department 唯一键 uk_user_dept 同场景）
     */
    @Test
    @Transactional
    public void testReassignSameDepartmentSucceeds() {
        User user = createTestUser();
        Long userId = user.getUserId();
        Long deptId = createTestDepartment().getDepartmentId();

        assertTrue(userService.assignDepartmentsToUser(userId, List.of(deptId)), "第一次分配部门应成功");
        assertTrue(userService.assignDepartmentsToUser(userId, List.of(deptId)), "重复分配相同部门应成功");

        List<UserDepartmentDto> depts = userDepartmentService.getDtosByUserId(userId);
        assertEquals(1, depts.size(), "重复分配后应仅保留1条部门关联");
        assertEquals(deptId, depts.get(0).getDepartmentId(), "部门ID应一致");
        // 第一个部门始终为主部门
        assertEquals(1, depts.get(0).getIsPrimary().intValue(), "主部门标记应保持正确");
    }

    /**
     * 测试删除用户关联时物理清空角色与部门，无软删残留
     */
    @Test
    @Transactional
    public void testDeleteUserAssociationsPhysicallyClears() {
        User user = createTestUser();
        Long userId = user.getUserId();
        Long roleId = createTestRole().getRoleId();
        Long deptId = createTestDepartment().getDepartmentId();

        assertTrue(userService.assignRolesToUser(userId, List.of(roleId)), "分配角色应成功");
        assertTrue(userService.assignDepartmentsToUser(userId, List.of(deptId)), "分配部门应成功");

        assertTrue(userService.deleteUserAssociations(userId), "删除用户关联应成功");
        assertEquals(0, userRoleService.getDtosByUserId(userId).size(), "角色关联应被清除");
        assertEquals(0, userDepartmentService.getDtosByUserId(userId).size(), "部门关联应被清除");
        assertEquals(0, userRoleMapper.selectCount(new QueryWrapper<UserRole>()
                        .eq("user_id", userId).eq("is_deleted", 1)).intValue(),
                "删除后不应残留软删角色记录");
    }

    // endregion

    // region 测试辅助方法
    // ===================================
    // 测试辅助方法
    // ===================================

    /**
     * 创建测试用户（唯一账号）
     *
     * @return 测试用户实体
     */
    private User createTestUser() {
        User user = new User();
        user.setUserAccount("TESTUSERROLE" + System.currentTimeMillis());
        user.setUserName("角色分配测试用户");
        user.setPassword("Test@123456");
        assertTrue(userService.saveWithHashedPassword(user), "创建测试用户应成功");
        return user;
    }

    /**
     * 创建测试角色（唯一名称）
     *
     * @return 测试角色实体
     */
    private Role createTestRole() {
        Role role = new Role();
        role.setRoleName("TESTROLEASSIGN" + System.currentTimeMillis());
        role.setRoleStatus(1);
        assertTrue(roleService.save(role), "创建测试角色应成功");
        return role;
    }

    /**
     * 创建测试部门（唯一名称）
     *
     * @return 测试部门实体
     */
    private Department createTestDepartment() {
        Department department = new Department();
        department.setDepartmentName("TESTDEPTASSIGN" + System.currentTimeMillis());
        department.setParentId(0L);
        department.setStatus(1);
        assertTrue(departmentService.save(department), "创建测试部门应成功");
        return department;
    }

    // endregion
}