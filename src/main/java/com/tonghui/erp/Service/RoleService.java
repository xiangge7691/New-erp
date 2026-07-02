package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.RoleDto;
import com.tonghui.erp.Common.Dto.System.RoleWithDetailsDto;
import com.tonghui.erp.Data.Entity.Role;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 角色服务接口
 * <p>
 * 提供角色相关的业务逻辑接口，包括角色的查询、权限分配等功能
 * </p>
 * 
 * @author 87954
 * @description 针对表【role(系统角色表)】的数据库操作Service
 * @createDate 2025-08-27 10:08:57
 */
public interface RoleService extends IService<Role> {
    
    //#region 角色查询接口
    // ===================================
    // 角色查询接口
    // ===================================
    
    /**
     * 高级查询角色（支持按角色名称进行模糊查询）
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
    PagedResult<RoleDto> advancedSearchRoles(
        String roleName,
        Long roleId,
        Integer status,
        Long userId,
        Long permissionId,
        int pageIndex,
        int pageSize);
        
    /**
     * 高级查询角色（默认分页参数）
     * @return 角色列表的分页结果
     */
    PagedResult<RoleDto> advancedSearchRoles();
    //#endregion
    
    //#region 角色详情接口
    // ===================================
    // 角色详情接口
    // ===================================
    
    /**
     * 获取角色详细信息
     * 根据角色ID获取该角色的完整信息，包括关联的用户和权限信息
     *
     * @param roleId 角色ID
     * @return 角色详细信息DTO
     */
    RoleDto getRoleDetails(Long roleId);
    //#endregion
    
    //#region 高级查询接口
    // ===================================
    // 高级查询接口
    // ===================================

    /**
     * 查询角色（支持多条件分页）
     */
    Page<Role> queryRoles(Role role, int pageNum, int pageSize);

    /**
     * 带子表查询角色
     */
    PagedResult<RoleWithDetailsDto> searchWithDetails(Role role, int pageNum, int pageSize);
    //#endregion

    //#region 数据转换接口
    // ===================================
    // 数据转换接口
    // ===================================
    
    /**
     * 将Role实体转换为RoleDto
     *
     * @param role Role实体
     * @return RoleDto对象
     */
    RoleDto convertToDto(Role role);
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
    boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds);
    //#endregion
}
