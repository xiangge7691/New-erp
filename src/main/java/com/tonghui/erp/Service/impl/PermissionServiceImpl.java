package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.Permission;
import com.tonghui.erp.Service.PermissionService;
import com.tonghui.erp.Data.mapper.PermissionMapper;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.PermissionDto;
import com.tonghui.erp.Common.Dto.System.RoleDto;
import com.tonghui.erp.Data.Entity.RolePerm;
import com.tonghui.erp.Data.Entity.Role;
import com.tonghui.erp.Service.RolePermService;
import com.tonghui.erp.Service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 权限服务实现类
 * <p>
 * 实现PermissionService接口，提供权限相关的业务逻辑处理，包括权限的查询、管理、树形结构构建等功能的具体实现
 * </p>
 * 
 * @author 87954
 * @description 针对表【permission(系统权限表)】的数据库操作Service实现
 * @createDate 2025-08-27 10:08:57
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission>
    implements PermissionService{
    
    @Autowired
    private RolePermService rolePermService;
    
    @Autowired
    @Lazy
    private RoleService roleService;

    //#region 权限查询接口
    // ===================================
    // 权限查询接口
    // ===================================
    
    /**
     * 高级查询权限（默认分页参数）
     * @return 权限列表的分页结果
     */
    @Override
    public PagedResult<PermissionDto> advancedSearchPermissions() {
        return advancedSearchPermissions(null, null, null, null, null, 0, -1);
    }

    /**
     * 高级查询权限（支持按名称、键名、状态、角色等条件查询）
     * 当不传递任何参数时，返回所有权限
     * 
     * @param permissionName 权限名称关键词，支持模糊查询
     * @param permKey 权限键名
     * @param permId 权限ID
     * @param status 权限状态
     * @param roleId 角色ID
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页数量，-1表示不分页返回所有结果
     * @return 权限列表的分页结果
     */
    @Override
    public PagedResult<PermissionDto> advancedSearchPermissions(
        String permissionName,
        String permKey,
        Long permId,
        Integer status,
        Long roleId,
        int pageIndex,
        int pageSize) {
        
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
        
        // 权限名称模糊查询
        if (permissionName != null && !permissionName.isEmpty()) {
            queryWrapper.like("perm_name", permissionName);
        }
        
        // 权限键名精确查询
        if (permKey != null && !permKey.isEmpty()) {
            queryWrapper.eq("perm_key", permKey);
        }
        
        // 权限ID精确查询
        if (permId != null) {
            queryWrapper.eq("perm_id", permId);
        }
        
        // 状态筛选
        if (status != null) {
            queryWrapper.eq("perm_status", status);
        }
        
        // 角色筛选
        if (roleId != null) {
            // 这里需要通过关联表查询
            List<RolePerm> rolePerms = rolePermService.list(new QueryWrapper<RolePerm>().eq("role_id", roleId));
            if (!rolePerms.isEmpty()) {
                List<Long> permIds = rolePerms.stream().map(RolePerm::getPermId).collect(Collectors.toList());
                queryWrapper.in("role_id", permIds);
            } else {
                // 如果没有找到关联，返回空结果
                return createEmptyPagedResult(pageIndex, pageSize);
            }
        }
        
        // 排序
        queryWrapper.orderByAsc("display_order");
        
        // 获取总数
        long totalCount = this.count(queryWrapper);
        
        // 分页处理
        List<Permission> permissionEntities;
        if (pageSize == -1) {
            permissionEntities = this.list(queryWrapper);
        } else {
            permissionEntities = this.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageIndex + 1, pageSize), 
                queryWrapper).getRecords();
        }
        
        // 转换为DTO
        List<PermissionDto> permissionDtos = permissionEntities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        // 构建分页结果
        PagedResult<PermissionDto> pagedResult = new PagedResult<>();
        pagedResult.setItems(permissionDtos);
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
    private PagedResult<PermissionDto> createEmptyPagedResult(int pageIndex, int pageSize) {
        PagedResult<PermissionDto> emptyResult = new PagedResult<>();
        emptyResult.setItems(new ArrayList<>());
        emptyResult.setTotalCount(0);
        emptyResult.setPageIndex(pageIndex);
        emptyResult.setPageSize(pageSize == -1 ? 0 : pageSize);
        return emptyResult;
    }
    //#endregion
    
    //#region 权限详情接口
    // ===================================
    // 权限详情接口
    // ===================================
    
    /**
     * 获取权限详细信息
     * 根据权限ID获取该权限的完整信息，包括关联的角色信息
     * @param permissionId 权限ID
     * @return 权限详细信息DTO
     */
    @Override
    public PermissionDto getPermissionDetails(Long permissionId) {
        Permission permissionEntity = this.getById(permissionId);
        return permissionEntity != null ? convertToDto(permissionEntity) : null;
    }
    //#endregion
    
    //#region 权限获取接口
    // ===================================
    // 权限获取接口
    // ===================================
    
    /**
     * 根据权限键获取权限
     * 通过权限键名精确查找权限
     * @param permKey 权限键名
     * @return 权限实体
     */
    @Override
    public Permission getByPermKey(String permKey) {
        return this.getOne(new QueryWrapper<Permission>().eq("perm_key", permKey));
    }
    
    /**
     * 根据父ID获取权限列表
     * @param parentId 父权限ID
     * @return 权限列表
     */
    @Override
    public List<Permission> getByParentId(Long parentId) {
        return this.list(new QueryWrapper<Permission>().eq("parent_id", parentId));
    }
    //#endregion
    
    //#region 权限树形结构接口
    // ===================================
    // 权限树形结构接口
    // ===================================
    
    /**
     * 获取权限树形结构
     * 将所有权限构建成树形结构，便于前端展示菜单和权限层级关系
     * @return 权限树DTO列表
     */
    @Override
    public List<PermissionDto> getPermissionTree() {
        List<Permission> permissions = this.list(new QueryWrapper<Permission>().orderByAsc("display_order"));
        
        List<PermissionDto> permissionDtos = permissions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        // 构建树结构
        return buildPermissionTree(permissionDtos);
    }
    
    /**
     * 构建权限树结构
     * @param permissions 权限DTO列表
     * @return 构建好的权限树
     */
    private List<PermissionDto> buildPermissionTree(List<PermissionDto> permissions) {
        // 建立字典以便快速查找
        Map<Long, PermissionDto> permissionDict = new HashMap<>();
        for (PermissionDto p : permissions) {
            permissionDict.put(p.getId(), p);
        }
        
        // 构建父子关系
        List<PermissionDto> rootPermissions = new ArrayList<>();
        for (PermissionDto perm : permissions) {
            if (perm.getParentId() != null && permissionDict.containsKey(perm.getParentId())) {
                permissionDict.get(perm.getParentId()).getChildren().add(perm);
            } else {
                rootPermissions.add(perm);
            }
        }
        
        // 对所有层级的子权限进行排序
        sortPermissionChildren(rootPermissions);
        
        return rootPermissions;
    }
    
    /**
     * 递归对权限的子权限进行排序
     * @param permissions 权限列表
     */
    private void sortPermissionChildren(List<PermissionDto> permissions) {
        for (PermissionDto perm : permissions) {
            if (!perm.getChildren().isEmpty()) {
                // 按显示顺序排序
                perm.getChildren().sort(Comparator.comparing(PermissionDto::getDisplayOrder));
                // 递归排序子权限的子权限
                sortPermissionChildren(perm.getChildren());
            }
        }
    }
    //#endregion
    
    //#region 数据转换接口
    // ===================================
    // 数据转换接口
    // ===================================
    
    /**
     * 将Permission实体转换为PermissionDto
     * @param permissionEntity Permission实体
     * @return PermissionDto对象
     */
    @Override
    public PermissionDto convertToDto(Permission permissionEntity) {
        if (permissionEntity == null) return null;
        
        PermissionDto permissionDto = new PermissionDto();
        permissionDto.setId(permissionEntity.getPermId());
        permissionDto.setPermKey(permissionEntity.getPermKey());
        permissionDto.setPermName(permissionEntity.getPermName());
        permissionDto.setPermType((String) permissionEntity.getPermType());
        permissionDto.setParentId(permissionEntity.getParentId());
        permissionDto.setDisplayOrder(permissionEntity.getDisplayOrder());
        permissionDto.setStatus(permissionEntity.getPermStatus()); // 映射permStatus到status
        permissionDto.setCreatedAt(permissionEntity.getCreatedAt());
        permissionDto.setUpdatedAt(permissionEntity.getUpdatedAt());
        
        // 获取关联的角色
        List<RolePerm> rolePerms = rolePermService.list(new QueryWrapper<RolePerm>().eq("perm_id", permissionEntity.getPermId()));
        List<RoleDto> roleDtos = new ArrayList<>();
        for (RolePerm rolePerm : rolePerms) {
            Role role = roleService.getById(rolePerm.getRoleId());
            if (role != null) {
                RoleDto roleDto = new RoleDto();
                roleDto.setRoleId(role.getRoleId());
                roleDto.setRoleName(role.getRoleName());
                roleDto.setRoleDesc(role.getRoleDesc());
                roleDto.setStatus(role.getRoleStatus());
                roleDto.setCreateTime(role.getCreateTime());
                roleDto.setUpdateTime(role.getUpdateTime());
                roleDtos.add(roleDto);
            }
        }
        permissionDto.setRoles(roleDtos);
        
        return permissionDto;
    }
    //#endregion
}




