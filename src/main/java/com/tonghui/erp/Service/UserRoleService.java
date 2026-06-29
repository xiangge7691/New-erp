package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.UserRoleDto;
import com.tonghui.erp.Data.Entity.UserRole;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户角色服务接口
 * <p>
 * 提供用户角色关联相关的业务逻辑接口，包括用户角色关联的查询、管理等功能
 * </p>
 * 
 * @author 87954
 * @description 针对表【user_role(用户-角色关联表)】的数据库操作Service
 * @createDate 2025-08-27 10:08:57
 */
public interface UserRoleService extends IService<UserRole> {
    
    //#region 基础查询接口
    // ===================================
    // 基础查询接口
    // ===================================
    
    /**
     * 根据用户ID获取所有角色关联
     *
     * @param userId 用户ID
     * @return 用户角色关联列表
     */
    List<UserRole> getByUserId(Long userId);

    /**
     * 根据角色ID获取所有用户关联
     *
     * @param roleId 角色ID
     * @return 用户角色关联列表
     */
    List<UserRole> getByRoleId(Long roleId);
    //#endregion

    //#region DTO查询接口
    // ===================================
    // DTO查询接口
    // ===================================
    
    /**
     * 根据用户ID获取所有角色关联DTO
     *
     * @param userId 用户ID
     * @return 用户角色关联DTO列表
     */
    List<UserRoleDto> getDtosByUserId(Long userId);

    /**
     * 根据角色ID获取所有用户关联DTO
     *
     * @param roleId 角色ID
     * @return 用户角色关联DTO列表
     */
    List<UserRoleDto> getDtosByRoleId(Long roleId);
    //#endregion

    //#region 数据转换接口
    // ===================================
    // 数据转换接口
    // ===================================
    
    /**
     * 将用户角色实体转换为DTO
     *
     * @param entity 用户角色实体
     * @return 用户角色DTO
     */
    UserRoleDto convertToDto(UserRole entity);
    //#endregion

    //#region 高级查询接口
    // ===================================
    // 高级查询接口
    // ===================================
    
    /**
     * 高级查询用户角色关联（支持按用户ID、角色ID和创建时间查询）
     * 当不传递任何参数时，返回所有用户角色关联
     *
     * @param userId           用户ID，用于筛选指定用户的关联
     * @param roleId           角色ID，用于筛选指定角色的关联
     * @param createdStartTime 创建时间起始，用于筛选此时间之后创建的关联
     * @param createdEndTime   创建时间结束，用于筛选此时间之前创建的关联
     * @param pageIndex        页码，从0开始
     * @param pageSize         每页数量，-1表示不分页返回所有结果
     * @return 用户角色关联列表的分页结果
     */
    PagedResult<UserRoleDto> advancedSearchUserRoles(
            Long userId,
            Long roleId,
            String createdStartTime,
            String createdEndTime,
            int pageIndex,
            int pageSize);

    /**
     * 根据查询条件获取用户角色关联（分页）
     *
     * @param userId           用户ID
     * @param roleId           角色ID
     * @param createdStartTime 创建时间起始
     * @param createdEndTime   创建时间结束
     * @param pageIndex        页码
     * @param pageSize         每页数量
     * @return 分页结果
     */
    PagedResult<UserRole> searchUserRoles(
            Long userId,
            Long roleId,
            String createdStartTime,
            String createdEndTime,
            int pageIndex,
            int pageSize);

    /**
     * 分页获取所有用户角色关联DTO
     *
     * @param pageIndex 页码
     * @param pageSize  每页数量
     * @return 分页结果
     */
    PagedResult<UserRoleDto> getPagedDto(int pageIndex, int pageSize);
    //#endregion
}
