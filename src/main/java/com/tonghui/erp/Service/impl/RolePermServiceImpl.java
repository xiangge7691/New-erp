package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.RolePerm;
import com.tonghui.erp.Service.RolePermService;
import com.tonghui.erp.Data.mapper.RolePermMapper;
import com.tonghui.erp.Common.Dto.PagedResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色权限服务实现类
 * <p>
 * 实现RolePermService接口，提供角色权限关联相关的业务逻辑处理，包括角色权限关联的查询、管理等功能的具体实现
 * </p>
 * 
 * @author 87954
 * @description 针对表【role_perm(角色-权限关联表)】的数据库操作Service实现
 * @createDate 2025-08-27 10:08:57
 */
@Service
public class RolePermServiceImpl extends ServiceImpl<RolePermMapper, RolePerm>
    implements RolePermService{

    //#region 权限查询接口
    // ===================================
    // 权限查询接口
    // ===================================
    
    /**
     * 根据角色ID获取权限列表
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    @Override
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        return this.list(new QueryWrapper<RolePerm>().eq("role_id", roleId))
                .stream()
                .map(RolePerm::getPermId)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据权限ID获取角色列表
     *
     * @param permId 权限ID
     * @return 角色ID列表
     */
    @Override
    public List<Long> getRoleIdsByPermissionId(Long permId) {
        return this.list(new QueryWrapper<RolePerm>().eq("perm_id", permId))
                .stream()
                .map(RolePerm::getRoleId)
                .collect(Collectors.toList());
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
     * @param permIds 权限ID列表
     * @return 操作是否成功
     */
    @Override
    public boolean assignPermissionsToRole(Long roleId, List<Long> permIds) {
        try {
            // 先删除现有权限关联
            this.remove(new QueryWrapper<RolePerm>().eq("role_id", roleId));
            
            // 添加新权限关联
            if (permIds != null && !permIds.isEmpty()) {
                for (Long permId : permIds) {
                    RolePerm rolePerm = new RolePerm();
                    rolePerm.setRoleId(roleId);
                    rolePerm.setPermId(permId);
                    rolePerm.setCreatedTime(LocalDateTime.now());
                    this.save(rolePerm);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 更新角色权限关联
     *
     * @param roleId 角色ID
     * @param permIds 权限ID列表
     * @return 操作是否成功
     */
    @Override
    public boolean updateRolePermissions(Long roleId, List<Long> permIds) {
        return assignPermissionsToRole(roleId, permIds);
    }
    //#endregion
    
    //#region 分页查询接口
    // ===================================
    // 分页查询接口
    // ===================================
    
    /**
     * 分页查询角色权限关联
     *
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页数量
     * @return 分页结果
     */
    @Override
    public PagedResult<RolePerm> getPaged(int pageIndex, int pageSize) {
        Page<RolePerm> page = new Page<>(pageIndex + 1, pageSize);
        Page<RolePerm> result = this.page(page, new QueryWrapper<>());
        
        PagedResult<RolePerm> pagedResult = new PagedResult<>();
        pagedResult.setItems(result.getRecords());
        pagedResult.setTotalCount(result.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize(pageSize);
        
        return pagedResult;
    }
    //#endregion
    
    //#region 关联查询接口
    // ===================================
    // 关联查询接口
    // ===================================
    
    /**
     * 根据角色ID获取权限关联
     *
     * @param roleId 角色ID
     * @return 角色权限关联列表
     */
    @Override
    public List<RolePerm> getByRoleId(Long roleId) {
        return this.list(new QueryWrapper<RolePerm>().eq("role_id", roleId));
    }
    
    /**
     * 根据权限ID获取角色关联
     *
     * @param permId 权限ID
     * @return 角色权限关联列表
     */
    @Override
    public List<RolePerm> getByPermId(Long permId) {
        return this.list(new QueryWrapper<RolePerm>().eq("perm_id", permId));
    }
    //#endregion
}




