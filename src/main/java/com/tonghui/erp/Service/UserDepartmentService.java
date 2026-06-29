package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.UserDepartmentDto;
import com.tonghui.erp.Data.Entity.UserDepartment;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户部门服务接口
 * <p>
 * 提供用户部门关联相关的业务逻辑接口，包括用户部门关联的查询、管理等功能
 * </p>
 * 
 * @author 87954
 * @description 针对表【user_department(用户-部门关联表)】的数据库操作Service
 * @createDate 2025-08-27 10:08:57
 */
public interface UserDepartmentService extends IService<UserDepartment> {
    
    //#region 基础查询接口
    // ===================================
    // 基础查询接口
    // ===================================
    
    /**
     * 根据用户ID获取所有部门关联
     *
     * @param userId 用户ID
     * @return 用户部门关联列表
     */
    List<UserDepartment> getByUserId(Long userId);

    /**
     * 根据部门ID获取所有用户关联
     *
     * @param departmentId 部门ID
     * @return 用户部门关联列表
     */
    List<UserDepartment> getByDepartmentId(Long departmentId);
    //#endregion

    //#region 主部门查询接口
    // ===================================
    // 主部门查询接口
    // ===================================
    
    /**
     * 获取用户的主部门
     *
     * @param userId 用户ID
     * @return 主部门关联
     */
    UserDepartment getPrimaryDepartment(Long userId);
    //#endregion

    //#region 分页查询接口
    // ===================================
    // 分页查询接口
    // ===================================
    
    /**
     * 分页查询用户部门关联
     *
     * @param pageIndex 页码（从0开始）
     * @param pageSize  每页数量
     * @return 分页结果
     */
    PagedResult<UserDepartment> getPaged(int pageIndex, int pageSize);

    /**
     * 根据查询条件获取用户部门关联（分页）
     *
     * @param userId       用户ID
     * @param departmentId 部门ID
     * @param isPrimary    是否主部门
     * @param pageIndex    页码
     * @param pageSize     每页数量
     * @return 分页结果
     */
    PagedResult<UserDepartment> searchUserDepartments(
            Long userId,
            Long departmentId,
            Integer isPrimary,
            String createdStartTime,
            String createdEndTime,
            int pageIndex,
            int pageSize);
    //#endregion

    //#region DTO分页查询接口
    // ===================================
    // DTO分页查询接口
    // ===================================
    
    /**
     * 分页获取所有用户部门关联DTO
     *
     * @param pageIndex 页码
     * @param pageSize  每页数量
     * @return 分页结果
     */
    PagedResult<UserDepartmentDto> getPagedDto(int pageIndex, int pageSize);
    //#endregion

    //#region DTO查询接口
    // ===================================
    // DTO查询接口
    // ===================================
    
    /**
     * 根据用户ID获取所有部门关联DTO
     *
     * @param userId 用户ID
     * @return 用户部门关联DTO列表
     */
    List<UserDepartmentDto> getDtosByUserId(Long userId);

    /**
     * 根据部门ID获取所有用户关联DTO
     *
     * @param departmentId 部门ID
     * @return 用户部门关联DTO列表
     */
    List<UserDepartmentDto> getDtosByDepartmentId(Long departmentId);

    /**
     * 获取用户的主部门DTO
     *
     * @param userId 用户ID
     * @return 主部门关联DTO
     */
    UserDepartmentDto getPrimaryDepartmentDto(Long userId);
    //#endregion
    
    //#region 数据转换接口
    // ===================================
    // 数据转换接口
    // ===================================
    
    /**
     * 将UserDepartment实体转换为UserDepartmentDto
     * 
     * @param entity UserDepartment实体
     * @return UserDepartmentDto对象
     */
    UserDepartmentDto convertToDto(UserDepartment entity);
    //#endregion
}
