package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Service.UserService;
import com.tonghui.erp.Data.mapper.UserMapper;
import com.tonghui.erp.Common.Dto.System.UserDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.utils.SoftDeleteCleanHelper;
import com.tonghui.erp.Common.Dto.System.UserWithDetailsDto;
import com.tonghui.erp.Common.Dto.System.DepartmentDto;
import com.tonghui.erp.Common.Dto.System.RoleDto;
import com.tonghui.erp.Common.utils.PasswordHasher;
import com.tonghui.erp.Data.Entity.UserDepartment;
import com.tonghui.erp.Data.Entity.UserRole;
import com.tonghui.erp.Data.Entity.Department;
import com.tonghui.erp.Data.Entity.Role;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Service.UserDepartmentService;
import com.tonghui.erp.Service.UserRoleService;
import com.tonghui.erp.Service.DepartmentService;
import com.tonghui.erp.Service.RoleService;
import com.tonghui.erp.Service.PersonnelFileService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * <p>
 * 实现UserService接口，提供用户相关的业务逻辑处理，包括用户的基本操作、查询、
 * 权限分配、密码验证、关联信息管理等功能的具体实现
 * </p>
 *
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService{

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 用户部门关联服务，用于管理用户与部门的关联关系 */
    @Autowired
    private UserDepartmentService userDepartmentService;

    /** 用户角色关联服务，用于管理用户与角色的关联关系 */
    @Autowired
    private UserRoleService userRoleService;

    /** 部门服务，用于获取部门详细信息 */
    @Autowired
    private DepartmentService departmentService;

    /** 角色服务，用于获取角色详细信息 */
    @Autowired
    private RoleService roleService;

    /** 人员档案服务，用于获取用户关联的人员档案信息 */
    @Autowired
    private PersonnelFileService personnelFileService;

    /** 软删除统一清理工具 */
    @Autowired
    private SoftDeleteCleanHelper softDeleteCleanHelper;

    // endregion

    // region 数据清理接口
    // ===================================
    // 数据清理接口
    // ===================================

    /**
     * 清理指定用户名下已被软删除的记录（释放唯一键约束）
     *
     * @param userAccount 用户名
     * @return 清理的记录数
     */
    public int cleanSoftDeletedByUserAccount(String userAccount) {
        return baseMapper.physicalDeleteByUserAccount(userAccount);
    }

    // endregion

    // region 基础操作接口
    // ===================================
    // 用户基础操作接口
    // ===================================

    /**
     * 获取所有用户DTO列表（不包含密码等敏感信息）
     *
     * @return 用户DTO列表
     */
    @Override
    public List<UserDto> listDto() {
        return this.list().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取用户DTO（包含关联的角色和部门信息）
     *
     * @param id 用户ID
     * @return 用户DTO对象，不存在则返回null
     */
    @Override
    public UserDto getDtoById(Long id) {
        User user = this.getById(id);
        return user != null ? convertToDtoWithAssociations(user) : null;
    }

    /**
     * 创建用户并哈希密码
     * <p>自动对密码进行Argon2哈希处理，并设置创建时间和更新时间</p>
     *
     * @param user 用户信息，密码为明文
     * @return 是否创建成功
     */
    @Override
    public boolean saveWithHashedPassword(User user) {
        // 对密码进行哈希处理
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(PasswordHasher.hashPassword(user.getPassword()));
        }
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        return this.save(user);
    }

    /**
     * 更新用户信息并哈希密码（如果提供了新密码）
     * <p>如果未提供密码，则保留数据库中原有密码不被覆盖</p>
     *
     * @param user 用户信息
     * @return 是否更新成功
     */
    @Override
    public boolean updateWithHashedPassword(User user) {
        // 如果密码被更新，则进行哈希处理
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(PasswordHasher.hashPassword(user.getPassword()));
        } else if (user.getPassword() == null) {
            // 没有提供密码，从数据库获取现有密码以避免覆盖
            User existingUser = this.getById(user.getUserId());
            if (existingUser != null) {
                user.setPassword(existingUser.getPassword());
            }
        }
        user.setUpdatedTime(LocalDateTime.now());
        return this.updateById(user);
    }

    /**
     * 选择性更新用户信息（仅更新非null字段）
     * <p>使用UpdateWrapper实现动态字段更新，密码字段单独处理哈希</p>
     *
     * @param user 用户信息，仅非null字段会被更新
     * @return 是否更新成功
     */
    @Override
    public boolean updateWithHashedPasswordSelective(User user) {
        // 使用UpdateWrapper进行选择性更新
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", user.getUserId());
        
        // 只更新非null字段
        if (user.getUserAccount() != null) {
            updateWrapper.set("user_account", user.getUserAccount());
        }
        if (user.getUserName() != null) {
            updateWrapper.set("user_name", user.getUserName());
        }
        if (user.getPhone() != null) {
            updateWrapper.set("phone", user.getPhone());
        }
        if (user.getGender() != null) {
            updateWrapper.set("gender", user.getGender());
        }
        if (user.getUserStatus() != null) {
            updateWrapper.set("user_status", user.getUserStatus());
        }
        if (user.getUserNotes() != null) {
            updateWrapper.set("user_notes", user.getUserNotes());
        }
        
        // 特殊处理密码字段
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            // 提供了非空密码，进行哈希处理并更新
            String hashedPassword = PasswordHasher.hashPassword(user.getPassword());
            updateWrapper.set("password", hashedPassword);
        }
        // 如果密码为null，不设置password字段，这样就不会更新密码
        
        updateWrapper.set("updated_time", LocalDateTime.now());
        
        return this.update(updateWrapper);
    }

    // endregion

    // region 查询接口
    // ===================================
    // 用户查询接口
    // ===================================

    /**
     * 高级查询用户（支持按部门、角色、状态和用户名进行模糊查询）
     * <p>当不传递任何参数时，返回所有用户</p>
     *
     * @param userName     用户名关键词，支持模糊查询用户名称和真实姓名
     * @param departmentId 部门ID，筛选指定部门的用户
     * @param roleId       角色ID，筛选具有指定角色的用户
     * @param status       用户状态，1为启用，0为禁用
     * @param userId       用户ID，用于精确查询单个用户
     * @param pageIndex    页码，从0开始
     * @param pageSize     每页数量，-1表示不分页返回所有结果
     * @return 用户列表的分页结果
     */
    @Override
    public PagedResult<UserDto> advancedSearchUsers(
            String userName,
            Long departmentId,
            Long roleId,
            Integer status,
            Long userId,
            int pageIndex,
            int pageSize) {

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        // 用户名模糊查询（支持用户名和真实姓名）
        if (userName != null && !userName.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like("user_name", userName)
                    .or()
                    .like("user_account", userName));
        }

        // 状态筛选
        if (status != null) {
            queryWrapper.eq("user_status", status);
        }

        // 用户ID精确查询
        if (userId != null) {
            queryWrapper.eq("user_id", userId);
        }

        // 部门筛选
        if (departmentId != null) {
            // 查询属于指定部门的用户
            List<UserDepartment> userDepartments = userDepartmentService.list(
                    new QueryWrapper<UserDepartment>().eq("department_id", departmentId));
            if (!userDepartments.isEmpty()) {
                List<Long> userIds = userDepartments.stream()
                        .map(UserDepartment::getUserId)
                        .collect(Collectors.toList());
                queryWrapper.in("user_id", userIds);
            } else {
                // 如果没有找到关联，返回空结果
                return createEmptyPagedResult(pageIndex, pageSize);
            }
        }

        // 角色筛选
        if (roleId != null) {
            // 查询拥有指定角色的用户
            List<UserRole> userRoles = userRoleService.list(
                    new QueryWrapper<UserRole>().eq("role_id", roleId));
            if (!userRoles.isEmpty()) {
                List<Long> userIds = userRoles.stream()
                        .map(UserRole::getUserId)
                        .collect(Collectors.toList());
                queryWrapper.in("user_id", userIds);
            } else {
                // 如果没有找到关联，返回空结果
                return createEmptyPagedResult(pageIndex, pageSize);
            }
        }

        // 获取总数
        long totalCount = this.count(queryWrapper);

        // 分页处理
        List<User> userEntities;
        if (pageSize == -1) {
            userEntities = this.list(queryWrapper);
        } else {
            userEntities = this.page(
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageIndex + 1, pageSize),
                    queryWrapper).getRecords();
        }

        // 转换为DTO
        List<UserDto> userDtos = userEntities.stream()
                .map(this::convertToDtoWithAssociations)
                .collect(Collectors.toList());

        // 构建分页结果
        PagedResult<UserDto> pagedResult = new PagedResult<>();
        pagedResult.setItems(userDtos);
        pagedResult.setTotalCount(totalCount);
        pagedResult.setPageIndex(pageIndex);

        if (pageSize == -1) {
            pagedResult.setPageSize((int) totalCount);
        } else {
            pagedResult.setPageSize(pageSize);
        }

        return pagedResult;
    }

    /**
     * 高级查询用户（默认分页参数，查询所有用户）
     *
     * @return 用户列表的分页结果
     */
    @Override
    public PagedResult<UserDto> advancedSearchUsers() {
        return advancedSearchUsers(null, null, null, null, null, 0, -1);
    }

    /**
     * 创建空的分页结果对象
     *
     * @param pageIndex 页码
     * @param pageSize  每页数量
     * @return 空的分页结果
     */
    private PagedResult<UserDto> createEmptyPagedResult(int pageIndex, int pageSize) {
        PagedResult<UserDto> emptyResult = new PagedResult<>();
        emptyResult.setItems(new ArrayList<>());
        emptyResult.setTotalCount(0);
        emptyResult.setPageIndex(pageIndex);
        emptyResult.setPageSize(pageSize == -1 ? 0 : pageSize);
        return emptyResult;
    }

    // endregion

    // region 验证接口
    // ===================================
    // 用户验证接口
    // ===================================

    /**
     * 验证用户密码
     * <p>通过用户名查找用户，然后使用Argon2算法验证密码</p>
     *
     * @param userName 用户名（登录账号）
     * @param password 明文密码
     * @return 验证结果，true表示验证成功，false表示验证失败
     */
    @Override
    public boolean validateUserPassword(String userName, String password) {
        User user = this.getOne(new QueryWrapper<User>().eq("user_account", userName));
        if (user == null) {
            return false;
        }
        return PasswordHasher.verifyPassword(password, user.getPassword());
    }

    // endregion

    // region 权限分配接口
    // ===================================
    // 用户权限分配接口
    // ===================================

    /**
     * 为用户分配角色
     * <p>先删除用户现有的所有角色关联，再创建新的角色关联</p>
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表，为空时将清除所有角色关联
     * @return 操作结果，true表示成功，false表示失败
     */
    @Override
    public boolean assignRolesToUser(Long userId, List<Long> roleIds) {
        try {
            // 先删除现有的角色关联
            userRoleService.remove(new QueryWrapper<UserRole>().eq("user_id", userId));

            // 如果roleIds不为null且不为空，则添加新的角色关联
            if (roleIds != null && !roleIds.isEmpty()) {
                // 添加新的角色关联
                for (Long roleId : roleIds) {
                    UserRole userRole = new UserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    userRole.setCreatedTime(LocalDateTime.now());
                    userRoleService.save(userRole);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 为用户分配部门
     * <p>先删除用户现有的所有部门关联，再创建新的部门关联，第一个部门将被设为主部门</p>
     *
     * @param userId         用户ID
     * @param departmentIds  部门ID列表，为空时将清除所有部门关联
     * @return 操作结果，true表示成功，false表示失败
     */
    @Override
    public boolean assignDepartmentsToUser(Long userId, List<Long> departmentIds) {
        try {
            // 先删除现有的部门关联
            userDepartmentService.remove(new QueryWrapper<UserDepartment>().eq("user_id", userId));

            if (departmentIds != null && !departmentIds.isEmpty()) {
                // 添加新的部门关联
                for (int i = 0; i < departmentIds.size(); i++) {
                    UserDepartment userDepartment = new UserDepartment();
                    userDepartment.setUserId(userId);
                    userDepartment.setDepartmentId(departmentIds.get(i));
                    userDepartment.setIsPrimary(i == 0 ? 1 : 0); // 第一个部门设为主部门
                    userDepartment.setCreatedTime(LocalDateTime.now());
                    userDepartmentService.save(userDepartment);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 更新用户角色关联
     * <p>先删除用户现有的所有角色关联，然后创建新的角色关联</p>
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     * @return 操作结果，true表示成功，false表示失败
     */
    @Override
    public boolean updateUserRoles(Long userId, List<Long> roleIds) {
        return assignRolesToUser(userId, roleIds);
    }

    /**
     * 更新用户部门关联
     * <p>先删除用户现有的所有部门关联，然后创建新的部门关联</p>
     *
     * @param userId         用户ID
     * @param departmentIds  部门ID列表
     * @return 操作结果，true表示成功，false表示失败
     */
    @Override
    public boolean updateUserDepartments(Long userId, List<Long> departmentIds) {
        return assignDepartmentsToUser(userId, departmentIds);
    }

    // endregion

    // region 关联信息管理接口
    // ===================================
    // 用户关联信息管理接口
    // ===================================

    /**
     * 删除用户相关的所有关联信息
     * <p>包括用户角色关联和用户部门关联</p>
     *
     * @param userId 用户ID
     * @return 操作结果，true表示成功，false表示失败
     */
    @Override
    public boolean deleteUserAssociations(Long userId) {
        try {
            // 删除用户角色关联
            userRoleService.remove(new QueryWrapper<UserRole>().eq("user_id", userId));

            // 删除用户部门关联
            userDepartmentService.remove(new QueryWrapper<UserDepartment>().eq("user_id", userId));

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // endregion

    // region 高级查询接口
    // ===================================
    // 高级查询接口
    // ===================================

    /**
     * 高级查询用户（支持按用户ID、账号、名称、状态条件组合查询）
     *
     * @param user     查询条件实体，非null字段将作为等值或模糊查询条件
     * @param pageNum  页码，从0开始
     * @param pageSize 每页数量
     * @return 用户分页结果
     */
    @Override
    public Page<User> queryUsers(User user, int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;
        Page<User> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<User> wrapper = new QueryWrapper<>();

        if (user != null) {
            if (user.getUserId() != null) {
                wrapper.eq("user_id", user.getUserId());
            }
            if (StringUtils.hasText(user.getUserAccount())) {
                wrapper.like("user_account", user.getUserAccount());
            }
            if (StringUtils.hasText(user.getUserName())) {
                wrapper.like("user_name", user.getUserName());
            }
            if (user.getUserStatus() != null) {
                wrapper.eq("user_status", user.getUserStatus());
            }
        }
        wrapper.orderByDesc("user_id");
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 查询用户列表并关联角色、部门和人员档案信息
     * <p>先分页查询用户主表数据，再批量查询关联的角色、部门和人员档案</p>
     *
     * @param user     查询条件实体
     * @param pageNum  页码，从0开始
     * @param pageSize 每页数量
     * @return 带子表关联数据的用户分页结果
     */
    @Override
    public PagedResult<UserWithDetailsDto> searchWithDetails(User user, int pageNum, int pageSize) {
        // 查询用户主表分页数据
        Page<User> parentPage = queryUsers(user, pageNum, pageSize);
        List<User> parents = parentPage.getRecords();

        PagedResult<UserWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的角色信息
        List<Long> parentIds = parents.stream().map(User::getUserId).collect(Collectors.toList());

        QueryWrapper<UserRole> roleWrapper = new QueryWrapper<>();
        roleWrapper.in("user_id", parentIds);
        Map<Long, List<UserRole>> rolesMap = userRoleService.list(roleWrapper).stream()
                .collect(Collectors.groupingBy(UserRole::getUserId));

        // 批量查询关联的部门信息
        QueryWrapper<UserDepartment> deptWrapper = new QueryWrapper<>();
        deptWrapper.in("user_id", parentIds);
        Map<Long, List<UserDepartment>> deptsMap = userDepartmentService.list(deptWrapper).stream()
                .collect(Collectors.groupingBy(UserDepartment::getUserId));

        // 批量查询关联的人员档案信息
        QueryWrapper<PersonnelFile> pfWrapper = new QueryWrapper<>();
        pfWrapper.in("user_id", parentIds);
        Map<Long, List<PersonnelFile>> pfMap = personnelFileService.list(pfWrapper).stream()
                .collect(Collectors.groupingBy(PersonnelFile::getUserId));

        // 组装带子表数据的DTO
        List<UserWithDetailsDto> dtos = parents.stream().map(parent -> {
            UserWithDetailsDto dto = new UserWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setRoles(rolesMap.getOrDefault(parent.getUserId(), Collections.emptyList()));
            dto.setDepartments(deptsMap.getOrDefault(parent.getUserId(), Collections.emptyList()));
            dto.setPersonnelFiles(pfMap.getOrDefault(parent.getUserId(), Collections.emptyList()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }

    // endregion

    // region 数据转换接口
    // ===================================
    // 数据转换接口
    // ===================================

    /**
     * 将User实体转换为UserDto（不包含关联信息）
     *
     * @param user User实体
     * @return UserDto对象
     */
    public UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getUserId());
        dto.setUserName(user.getUserAccount());
        dto.setName(user.getUserName());
        dto.setPhone(user.getPhone());
        dto.setGender(user.getGender());
        dto.setStatus(user.getUserStatus());
        dto.setNotes(user.getUserNotes());
        dto.setCreatedTime(user.getCreatedTime());
        dto.setUpdatedTime(user.getUpdatedTime());
        return dto;
    }

    /**
     * 将User实体转换为UserDto，包含关联的角色和部门信息
     *
     * @param user User实体
     * @return UserDto对象，包含角色列表、部门列表和主部门信息
     */
    private UserDto convertToDtoWithAssociations(User user) {
        UserDto dto = convertToDto(user);

        // 获取用户部门信息
        List<UserDepartment> userDepartments = userDepartmentService
                .list(new QueryWrapper<UserDepartment>().eq("user_id", user.getUserId()));

        List<DepartmentDto> departmentDtos = new ArrayList<>();
        DepartmentDto primaryDepartmentDto = null;

        for (UserDepartment userDepartment : userDepartments) {
            Department department = departmentService.getById(userDepartment.getDepartmentId());
            if (department != null) {
                DepartmentDto departmentDto = new DepartmentDto();
                departmentDto.setId(department.getDepartmentId());
                departmentDto.setDepartmentName(department.getDepartmentName());
                // 清除不存在的字段
                departmentDto.setCreatedTime(null);
                departmentDto.setUpdatedTime(null);
                departmentDtos.add(departmentDto);

                // 设置主部门
                if (userDepartment.getIsPrimary() != null && userDepartment.getIsPrimary() == 1) {
                    primaryDepartmentDto = departmentDto;
                }
            }
        }

        dto.setDepartments(departmentDtos);
        dto.setPrimaryDepartment(primaryDepartmentDto);

        // 获取用户角色信息
        List<UserRole> userRoles = userRoleService
                .list(new QueryWrapper<UserRole>().eq("user_id", user.getUserId()));

        List<RoleDto> roleDtos = new ArrayList<>();
        for (UserRole userRole : userRoles) {
            Role role = roleService.getById(userRole.getRoleId());
            if (role != null) {
                RoleDto roleDto = new RoleDto();
                roleDto.setRoleId(role.getRoleId());
                roleDto.setRoleName(role.getRoleName());
                roleDto.setRoleDesc(role.getRoleDesc());
                roleDto.setStatus(role.getRoleStatus());
                roleDto.setCreatedTime(role.getCreatedTime());
                roleDto.setUpdatedTime(role.getUpdatedTime());
                // 清除不需要的字段
                roleDto.setUsers(null);
                roleDtos.add(roleDto);
            }
        }

        dto.setRoles(roleDtos);

        return dto;
    }

    // endregion
}
