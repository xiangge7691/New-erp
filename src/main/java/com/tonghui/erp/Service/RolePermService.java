package com.tonghui.erp.Service;

import com.tonghui.erp.Data.Entity.RolePerm;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PagedResult;

import java.util.List;

/**
 * 角色权限服务接口
 * <p>
 * 提供角色权限关联相关的业务逻辑接口，包括角色权限关联的查询、管理等功能
 * </p>
 * 
 * @author 87954
 * @description 针对表【role_perm(角色-权限关联表)】的数据库操作Service
 * @createDate 2025-08-27 10:08:57
 */
public interface RolePermService extends IService<RolePerm> {
    
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
    List<Long> getPermissionIdsByRoleId(Long roleId);
    
    /**
     * 根据权限ID获取角色列表
     *
     * @param permId 权限ID
     * @return 角色ID列表
     */
    List<Long> getRoleIdsByPermissionId(Long permId);
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
    boolean assignPermissionsToRole(Long roleId, List<Long> permIds);
    
    /**
     * 更新角色权限关联
     *
     * @param roleId 角色ID
     * @param permIds 权限ID列表
     * @return 操作是否成功
     */
    boolean updateRolePermissions(Long roleId, List<Long> permIds);
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
    PagedResult<RolePerm> getPaged(int pageIndex, int pageSize);
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
    List<RolePerm> getByRoleId(Long roleId);
    
    /**
     * 根据权限ID获取角色关联
     *
     * @param permId 权限ID
     * @return 角色权限关联列表
     */
    List<RolePerm> getByPermId(Long permId);
    //#endregion
}
