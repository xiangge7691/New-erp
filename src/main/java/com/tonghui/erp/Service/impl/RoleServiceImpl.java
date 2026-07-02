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
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 * <p>
 * 实现RoleService接口，提供角色相关的业务逻辑处理，包括角色的查询、权限分配等功能的具体实现
 * </p>
 * 
 * @author 87954
 * @description 针对表【role(系统角色表)】的数据库操作Service实现
 * @createDate 2025-08-27 10:08:57
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role>
    implements RoleService{
    
    @Autowired
    private UserRoleService userRoleService;
    
    @Autowired
    private RolePermService rolePermService;
    
    @Autowired
    @Lazy
    private UserService userService;
    
    @Autowired
    private PermissionService permissionService;

    @Autowired
    private Converters converters;

    //#region 角色查询接口
    // ===================================
    // 角色查询接口
    // ===================================
    
    /**
     * 高级查询角色（默认分页参数）
     * @return 角色列表的分页结果
     */
    @Override
    public PagedResult<RoleDto> advancedSearchRoles() {
        return advancedSearchRoles(null, null, null, null, null, 0, -1);
    }

    /**
     * 高级查询角色（支持按名称、状态和ID进行查询）
     * 当不传递任何参数时，返回所有角色
     * 
     * @param roleName 角色名称关键词，支持模糊查询
     * @param roleId 角色ID，用于精确查询单个角色
     * @param status 角色状态，1为启用，0为禁用
     * @param userId 用户ID，筛选具有指定用户的角色
     * @param permissionId 权限ID，筛选具有指定权限的角色
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页数量，-1表示不分页返回所有结果
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
            // 这里需要通过关联表查询
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
            // 这里需要通过关联表查询
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
     * 创建空的分页结果
     * @param pageIndex 页码
     * @param pageSize 每页数量
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
    //#endregion
    
    //#region 角色详情接口
    // ===================================
    // 角色详情接口
    // ===================================
    
    /**
     * 获取角色详细信息
     * 根据角色ID获取该角色的完整信息，包括关联的用户和权限信息
     * @param roleId 角色ID
     * @return 角色详细信息DTO
     */
    @Override
    public RoleDto getRoleDetails(Long roleId) {
        Role roleEntity = this.getById(roleId);
        return roleEntity != null ? convertToDto(roleEntity) : null;
    }
    //#endregion
    
    //#region 高级查询接口
    // ===================================
    // 高级查询接口
    // ===================================

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

    @Override
    public PagedResult<RoleWithDetailsDto> searchWithDetails(Role role, int pageNum, int pageSize) {
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

        List<Long> parentIds = parents.stream().map(Role::getRoleId).collect(Collectors.toList());

        QueryWrapper<RolePerm> permWrapper = new QueryWrapper<>();
        permWrapper.in("role_id", parentIds);
        Map<Long, List<RolePerm>> permsMap = rolePermService.list(permWrapper).stream()
                .collect(Collectors.groupingBy(RolePerm::getRoleId));

        QueryWrapper<UserRole> urWrapper = new QueryWrapper<>();
        urWrapper.in("role_id", parentIds);
        Map<Long, List<UserRole>> urMap = userRoleService.list(urWrapper).stream()
                .collect(Collectors.groupingBy(UserRole::getRoleId));

        List<RoleWithDetailsDto> dtos = parents.stream().map(parent -> {
            RoleWithDetailsDto dto = new RoleWithDetailsDto();
            BeanUtils.copyProperties(parent, dto);
            dto.setPermissions(permsMap.getOrDefault(parent.getRoleId(), Collections.emptyList()));
            dto.setUserRoles(urMap.getOrDefault(parent.getRoleId(), Collections.emptyList()));
            return dto;
        }).collect(Collectors.toList());

        result.setItems(dtos);
        result.setTotalCount(parentPage.getTotal());
        result.setPageIndex(pageNum);
        result.setPageSize(pageSize);
        return result;
    }
    //#endregion

    //#region 数据转换接口
    // ===================================
    // 数据转换接口
    // ===================================
    
    /**
     * 将角色实体转换为DTO
     * @param roleEntity 角色实体
     * @return 角色DTO对象
     */
    @Override
    public RoleDto convertToDto(Role roleEntity) {
        if (roleEntity == null) return null;
        
        RoleDto roleDto = converters.toRoleDto(roleEntity);
        
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
    //#endregion
    
    //#region 权限分配接口
    // ===================================
    // 权限分配接口
    // ===================================
    
    /**
     * 为角色分配权限
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 操作是否成功
     */
    @Override
    public boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        return rolePermService.assignPermissionsToRole(roleId, permissionIds);
    }
    //#endregion
}




