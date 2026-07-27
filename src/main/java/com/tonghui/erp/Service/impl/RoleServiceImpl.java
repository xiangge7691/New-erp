package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.Role;
import com.tonghui.erp.Service.RoleService;
import com.tonghui.erp.Data.mapper.RoleMapper;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.RoleDto;
import com.tonghui.erp.Common.Dto.System.RoleWithDetailsDto;
import com.tonghui.erp.Common.Dto.System.UserDto;
import com.tonghui.erp.Common.Dto.System.PermissionDto;
import com.tonghui.erp.Data.Entity.UserRole;
import com.tonghui.erp.Data.Entity.RolePerm;
import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.Entity.Permission;
import com.tonghui.erp.Service.UserRoleService;
import com.tonghui.erp.Service.RolePermService;
import com.tonghui.erp.Service.UserService;
import com.tonghui.erp.Service.PermissionService;
import com.tonghui.erp.Common.Mapper.Converters;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 * <p>
 * 实现RoleService接口，提供角色相关的业务逻辑处理，包括角色的查询、
 * 权限分配、数据转换等功能的具体实现
 * </p>
 *
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role>
    implements RoleService{
    
    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /** 用户角色关联服务，用于查询角色关联的用户 */
    @Autowired
    private UserRoleService userRoleService;
    
    /** 角色权限关联服务，用于查询和分配角色的权限 */
    @Autowired
    private RolePermService rolePermService;
    
    /** 用户服务，用于获取角色关联的用户信息（使用@Lazy避免循环依赖） */
    @Autowired
    @Lazy
    private UserService userService;
    
    /** 权限服务，用于获取权限详细信息 */
    @Autowired
    private PermissionService permissionService;

    /** 实体转换工具，用于Entity到DTO的转换 */
    @Autowired
    private Converters converters;

    // endregion

    // region 数据清理接口
    // ===================================
    // 数据清理接口
    // ===================================

    /**
     * 清理指定角色名称下已被软删除的记录（释放唯一键约束）
     *
     * @param roleName 角色名称
     * @return 清理的记录数
     */
    public int cleanSoftDeletedByRoleName(String roleName) {
        return baseMapper.physicalDeleteByRoleName(roleName);
    }

    // endregion

    // region 角色查询接口
    // ===================================
    // 角色查询接口
    // ===================================

    /**
     * 高级查询角色（默认分页参数，查询所有角色）
     *
     * @return 角色列表的分页结果
     */
    @Override
    public PagedResult<RoleDto> advancedSearchRoles() {
        return advancedSearchRoles(null, null, null, null, null, 0, -1);
    }

    /**
     * 高级查询角色（支持按名称、状态、用户ID和权限ID进行查询）
     * <p>当不传递任何参数时，返回所有角色</p>
     *
     * @param roleName     角色名称关键词，支持模糊查询
     * @param roleId       角色ID，用于精确查询单个角色
     * @param status       角色状态，1为启用，0为禁用
     * @param userId       用户ID，筛选具有指定用户的角色
     * @param permissionId 权限ID，筛选具有指定权限的角色
     * @param pageIndex    页码，从0开始
     * @param pageSize     每页数量，-1表示不分页返回所有结果
     * @return 角色列表的分页结果
     */
    @Override
    public PagedResult<RoleDto> advancedSearchRoles(
        String roleName,
        Long roleId,
        Integer status,
        Long userId,
        Long permissionId,
        int pageIndex,
        int pageSize) {
        
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        
        // 角色名称模糊查询
        if (roleName != null && !roleName.isEmpty()) {
            queryWrapper.like("role_name", roleName);
        }
        
        // 角色ID精确查询
        if (roleId != null) {
            queryWrapper.eq("role_id", roleId);
        }
        
        // 状态筛选
        if (status != null) {
            queryWrapper.eq("role_status", status);
        }
        
        // 用户筛选
        if (userId != null) {
            // 通过关联表查询拥有指定用户的角色
            List<UserRole> userRoles = userRoleService.list(new QueryWrapper<UserRole>().eq("user_id", userId));
            if (!userRoles.isEmpty()) {
                List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
                queryWrapper.in("role_id", roleIds);
            } else {
                // 如果没有找到关联，返回空结果
                return createEmptyPagedResult(pageIndex, pageSize);
            }
        }
        
        // 权限筛选
        if (permissionId != null) {
            // 通过关联表查询拥有指定权限的角色
            List<RolePerm> rolePerms = rolePermService.list(new QueryWrapper<RolePerm>().eq("perm_id", permissionId));
            if (!rolePerms.isEmpty()) {
                List<Long> roleIds = rolePerms.stream().map(RolePerm::getRoleId).collect(Collectors.toList());
                queryWrapper.in("role_id", roleIds);
            } else {
                // 如果没有找到关联，返回空结果
                return createEmptyPagedResult(pageIndex, pageSize);
            }
        }
        
        // 获取总数
        long totalCount = this.count(queryWrapper);
        
        // 分页处理
        List<Role> roleEntities;
        if (pageSize == -1) {
            roleEntities = this.list(queryWrapper);
        } else {
            roleEntities = this.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageIndex + 1, pageSize), 
                queryWrapper).getRecords();
        }
        
        // 转换为DTO
        List<RoleDto> roleDtos = roleEntities.stream()
                .map(converters::toRoleDto)
                .collect(Collectors.toList());

        // 批量加载权限信息并填充到DTO
        List<Long> roleIds = roleEntities.stream().map(Role::getRoleId).collect(Collectors.toList());
        if (!roleIds.isEmpty()) {
            QueryWrapper<RolePerm> permQuery = new QueryWrapper<>();
            permQuery.in("role_id", roleIds);
            Map<Long, List<RolePerm>> rolePermsMap = rolePermService.list(permQuery).stream()
                    .collect(Collectors.groupingBy(RolePerm::getRoleId));

            // 批量加载权限详情
            Set<Long> allPermIds = rolePermsMap.values().stream()
                    .flatMap(List::stream)
                    .map(RolePerm::getPermId)
                    .collect(Collectors.toSet());
            Map<Long, Permission> permMap = allPermIds.isEmpty() ? java.util.Map.of()
                    : permissionService.listByIds(allPermIds).stream()
                    .collect(Collectors.toMap(Permission::getPermId, p -> p));

            // 填充每个角色的权限列表
            for (RoleDto dto : roleDtos) {
                List<RolePerm> rolePerms = rolePermsMap.getOrDefault(dto.getRoleId(), Collections.emptyList());
                List<PermissionDto> permDtos = rolePerms.stream()
                        .map(rp -> permMap.get(rp.getPermId()))
                        .filter(java.util.Objects::nonNull)
                        .map(converters::toPermissionDto)
                        .collect(Collectors.toList());
                dto.setPermissions(permDtos);
            }
        }
        
        // 构建分页结果
        PagedResult<RoleDto> pagedResult = new PagedResult<>();
        pagedResult.setItems(roleDtos);
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
     * 创建空的分页结果对象
     *
     * @param pageIndex 页码
     * @param pageSize  每页数量
     * @return 空的分页结果
     */
    private PagedResult<RoleDto> createEmptyPagedResult(int pageIndex, int pageSize) {
        PagedResult<RoleDto> emptyResult = new PagedResult<>();
        emptyResult.setItems(new ArrayList<>());
        emptyResult.setTotalCount(0);
        emptyResult.setPageIndex(pageIndex);
        emptyResult.setPageSize(pageSize == -1 ? 0 : pageSize);
        return emptyResult;
    }

    // endregion
    
    // region 角色详情接口
    // ===================================
    // 角色详情接口
    // ===================================

    /**
     * 获取角色详细信息
     * <p>根据角色ID获取该角色的完整信息，包括关联的用户和权限信息</p>
     *
     * @param roleId 角色ID
     * @return 角色详细信息DTO，不存在则返回null
     */
    @Override
    public RoleDto getRoleDetails(Long roleId) {
        Role roleEntity = this.getById(roleId);
        return roleEntity != null ? convertToDto(roleEntity) : null;
    }

    // endregion
    
    // region 高级查询接口
    // ===================================
    // 高级查询接口
    // ===================================

    /**
     * 高级查询角色（支持按角色ID、名称、状态条件组合查询）
     *
     * @param role     查询条件实体，非null字段将作为等值或模糊查询条件
     * @param pageNum  页码，从0开始
     * @param pageSize 每页数量
     * @return 角色分页结果
     */
    @Override
    public Page<Role> queryRoles(Role role, int pageNum, int pageSize) {
        int actualPageNum = pageNum + 1;
        Page<Role> page = new Page<>(actualPageNum, pageSize);
        QueryWrapper<Role> wrapper = new QueryWrapper<>();

        if (role != null) {
            if (role.getRoleId() != null) {
                wrapper.eq("role_id", role.getRoleId());
            }
            if (StringUtils.hasText(role.getRoleName())) {
                wrapper.like("role_name", role.getRoleName());
            }
            if (role.getRoleStatus() != null) {
                wrapper.eq("role_status", role.getRoleStatus());
            }
        }
        wrapper.orderByDesc("role_id");
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 查询角色列表并关联权限和用户角色信息
     * <p>先分页查询角色主表数据，再批量查询关联的权限和用户角色</p>
     *
     * @param role     查询条件实体
     * @param pageNum  页码，从0开始
     * @param pageSize 每页数量
     * @return 带子表关联数据的角色分页结果
     */
    @Override
    public PagedResult<RoleWithDetailsDto> searchWithDetails(Role role, int pageNum, int pageSize) {
        // 查询角色主表分页数据
        Page<Role> parentPage = queryRoles(role, pageNum, pageSize);
        List<Role> parents = parentPage.getRecords();

        PagedResult<RoleWithDetailsDto> result = new PagedResult<>();
        if (parents.isEmpty()) {
            result.setItems(List.of());
            result.setTotalCount(parentPage.getTotal());
            result.setPageIndex(pageNum);
            result.setPageSize(pageSize);
            return result;
        }

        // 批量查询关联的权限信息
        List<Long> parentIds = parents.stream().map(Role::getRoleId).collect(Collectors.toList());

        QueryWrapper<RolePerm> permWrapper = new QueryWrapper<>();
        permWrapper.in("role_id", parentIds);
        Map<Long, List<RolePerm>> permsMap = rolePermService.list(permWrapper).stream()
                .collect(Collectors.groupingBy(RolePerm::getRoleId));

        // 批量查询权限详情
        Set<Long> allPermIds = permsMap.values().stream()
                .flatMap(List::stream)
                .map(RolePerm::getPermId)
                .collect(Collectors.toSet());
        Map<Long, Permission> permDetailsMap = allPermIds.isEmpty() ? java.util.Map.of()
                : permissionService.listByIds(allPermIds).stream()
                .collect(Collectors.toMap(Permission::getPermId, p -> p));

        // 批量查询关联的用户角色信息
        QueryWrapper<UserRole> urWrapper = new QueryWrapper<>();
        urWrapper.in("role_id", parentIds);
        Map<Long, List<UserRole>> urMap = userRoleService.list(urWrapper).stream()
                .collect(Collectors.groupingBy(UserRole::getRoleId));

        // 组装带子表数据的DTO
        List<RoleWithDetailsDto> dtos = parents.stream().map(parent -> {
            RoleWithDetailsDto dto = new RoleWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            
            // 将RolePerm转换为PermissionDto
            List<RolePerm> rolePerms = permsMap.getOrDefault(parent.getRoleId(), Collections.emptyList());
            List<PermissionDto> permDtos = rolePerms.stream()
                    .map(rp -> permDetailsMap.get(rp.getPermId()))
                    .filter(Objects::nonNull)
                    .map(converters::toPermissionDto)
                    .collect(Collectors.toList());
            dto.setPermissions(permDtos);
            
            dto.setUserRoles(urMap.getOrDefault(parent.getRoleId(), Collections.emptyList()));
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
     * 将角色实体转换为DTO（包含关联的用户和权限信息）
     *
     * @param roleEntity 角色实体
     * @return 角色DTO对象，包含用户列表和权限列表
     */
    @Override
    public RoleDto convertToDto(Role roleEntity) {
        if (roleEntity == null) return null;
        
        // 基础字段转换
        RoleDto roleDto = converters.toRoleDto(roleEntity);
        
        // 查询并设置关联的用户信息
        List<UserRole> userRoles = userRoleService.list(new QueryWrapper<UserRole>().eq("role_id", roleEntity.getRoleId()));
        List<UserDto> userDtos = new ArrayList<>();
        for (UserRole userRole : userRoles) {
            User user = userService.getById(userRole.getUserId());
            if (user != null) {
                UserDto userDto = converters.toUserDto(user);
                userDtos.add(userDto);
            }
        }
        roleDto.setUsers(userDtos);
        
        // 查询并设置关联的权限信息
        List<RolePerm> rolePerms = rolePermService.list(new QueryWrapper<RolePerm>().eq("role_id", roleEntity.getRoleId()));
        List<PermissionDto> permissionDtos = new ArrayList<>();
        for (RolePerm rolePerm : rolePerms) {
            Permission permission = permissionService.getById(rolePerm.getPermId());
            if (permission != null) {
                PermissionDto permissionDto = converters.toPermissionDto(permission);
                permissionDtos.add(permissionDto);
            }
        }
        roleDto.setPermissions(permissionDtos);
        
        return roleDto;
    }

    // endregion
    
    // region 权限分配接口
    // ===================================
    // 权限分配接口
    // ===================================

    /**
     * 为角色分配权限
     *
     * @param roleId        角色ID
     * @param permissionIds 权限ID列表
     * @return 操作是否成功
     */
    @Override
    public boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        return rolePermService.assignPermissionsToRole(roleId, permissionIds);
    }

    // endregion
}
