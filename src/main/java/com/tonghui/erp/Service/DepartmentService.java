package com.tonghui.erp.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tonghui.erp.Data.Entity.Department;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.DepartmentDto;
import com.tonghui.erp.Common.Dto.System.DepartmentWithDetailsDto;

import java.util.List;

/**
 * 部门服务接口
 * <p>
 * 提供部门相关的业务逻辑接口，包括部门的查询、管理等功能
 * </p>
 * 
 * @author 87954
 * @description 针对表【department(部门信息表)】的数据库操作Service
 * @createDate 2025-08-27 10:08:57
 */
public interface DepartmentService extends IService<Department> {
    
    //#region 基础操作接口
    // ===================================
    // 部门基础操作接口
    // ===================================
    
    /**
     * 获取所有部门DTO列表
     *
     * @return 部门DTO列表
     */
    List<DepartmentDto> listDto();

    /**
     * 根据ID获取部门DTO
     *
     * @param id 部门ID
     * @return 部门DTO对象
     */
    DepartmentDto getDtoById(Long id);
    //#endregion

    //#region 部门查询接口
    // ===================================
    // 部门查询接口
    // ===================================
    
    /**
     * 高级查询部门（支持按名称、状态等条件查询）
     * 当不传递任何参数时，返回所有部门
     *
     * @param departmentName 部门名称关键词，支持模糊查询
     * @param status 部门状态
     * @param pageIndex 页码，从0开始
     * @param pageSize 每页数量，-1表示不分页返回所有结果
     * @return 部门列表的分页结果
     */
    PagedResult<DepartmentDto> advancedSearchDepartments(
        String departmentName,
        Integer status,
        int pageIndex,
        int pageSize);
        
    /**
     * 高级查询部门（默认分页参数）
     * @return 部门列表的分页结果
     */
    PagedResult<DepartmentDto> advancedSearchDepartments();
    //#endregion
    
    //#region 高级查询接口
    // ===================================
    // 高级查询接口
    // ===================================

    /**
     * 查询部门（支持多条件分页）
     */
    Page<Department> queryDepartments(Department department, int pageNum, int pageSize);

    /**
     * 带子表查询部门
     */
    PagedResult<DepartmentWithDetailsDto> searchWithDetails(Department department, int pageNum, int pageSize);
    //#endregion

    //#region 数据转换接口
    // ===================================
    // 数据转换接口
    // ===================================
    
    /**
     * 将Department实体转换为DepartmentDto
     *
     * @param department Department实体
     * @return DepartmentDto对象
     */
    DepartmentDto convertToDto(Department department);
    //#endregion
    
    //#region 部门信息获取接口
    // ===================================
    // 部门信息获取接口
    // ===================================
    
    /**
     * 根据部门名称获取部门
     *
     * @param departmentName 部门名称
     * @return 部门实体
     */
    Department getByDepartmentName(String departmentName);
    //#endregion
    
    //#region 部门关联检查接口
    // ===================================
    // 部门关联检查接口
    // ===================================
    
    /**
     * 检查部门是否有关联的用户
     *
     * @param departmentId 部门ID
     * @return 是否有关联用户
     */
    boolean hasUserDepartments(Long departmentId);
    //#endregion
    
    //#region 部门详情接口
    // ===================================
    // 部门详情接口
    // ===================================
    
    /**
     * 获取部门详细信息
     *
     * @param departmentId 部门ID
     * @return 部门详细信息
     */
    DepartmentDto getDepartmentDetails(Long departmentId);
    //#endregion
}
