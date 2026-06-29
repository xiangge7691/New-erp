package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.PermissionDto;
import com.tonghui.erp.Data.Entity.Permission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 权限服务接口
 * <p>
 * 提供权限相关的业务逻辑接口，包括权限的查询、管理、树形结构构建等功能
 * </p>
 * 
 * @author 87954
 * @description 针对表【permission(系统权限表)】的数据库操作Service
 * @createDate 2025-08-27 10:08:57
 */
public interface PermissionService extends IService<Permission> {
    
    //#region 权限查询接口
    // ===================================
    // 权限查询接口
    // ===================================
    
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
    PagedResult<PermissionDto> advancedSearchPermissions(
        String permissionName,
        String permKey,
        Long permId,
        Integer status,
        Long roleId,
        int pageIndex,
        int pageSize);
        
    /**
     * 高级查询权限（默认分页参数）
     * @return 权限列表的分页结果
     */
    PagedResult<PermissionDto> advancedSearchPermissions();
    //#endregion
    
    //#region 权限详情接口
    // ===================================
    // 权限详情接口
    // ===================================
    
    /**
     * 获取权限详细信息
     * 根据权限ID获取该权限的完整信息，包括关联的角色信息
     *
     * @param permissionId 权限ID
     * @return 权限详细信息DTO
     */
    PermissionDto getPermissionDetails(Long permissionId);
    //#endregion
    
    //#region 权限获取接口
    // ===================================
    // 权限获取接口
    // ===================================
    
    /**
     * 根据权限键获取权限
     * 通过权限键名精确查找权限
     *
     * @param permKey 权限键名
     * @return 权限实体
     */
    Permission getByPermKey(String permKey);
    
    /**
     * 根据父ID获取权限列表
     * 
     * @param parentId 父权限ID
     * @return 权限列表
     */
    List<Permission> getByParentId(Long parentId);
    //#endregion
    
    //#region 权限树形结构接口
    // ===================================
    // 权限树形结构接口
    // ===================================
    
    /**
     * 获取权限树形结构
     * 将所有权限构建成树形结构，便于前端展示菜单和权限层级关系
     *
     * @return 权限树DTO列表
     */
    List<PermissionDto> getPermissionTree();
    //#endregion
    
    //#region 数据转换接口
    // ===================================
    // 数据转换接口
    // ===================================
    
    /**
     * 将Permission实体转换为PermissionDto
     *
     * @param permission Permission实体
     * @return PermissionDto对象
     */
    PermissionDto convertToDto(Permission permission);
    //#endregion
}
