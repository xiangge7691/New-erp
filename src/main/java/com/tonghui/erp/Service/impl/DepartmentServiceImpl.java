package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.Department;
import com.tonghui.erp.Service.DepartmentService;
import com.tonghui.erp.Data.mapper.DepartmentMapper;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.DepartmentDto;
import com.tonghui.erp.Service.UserDepartmentService;
import com.tonghui.erp.Common.Mapper.Converters;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门服务实现类
 * <p>
 * 实现DepartmentService接口，提供部门相关的业务逻辑处理，包括部门的查询、管理等功能的具体实现
 * </p>
 * 
 * @author 87954
 * @description 针对表【department(部门信息表)】的数据库操作Service实现
 * @createDate 2025-08-27 10:08:57
 */
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department>
    implements DepartmentService{
    
    @Autowired
    private UserDepartmentService userDepartmentService;

    @Autowired
    private Converters converters;
    
    //#region 基础操作接口
    // ===================================
    // 部门基础操作接口
    // ===================================
    
    /**
     * 获取所有部门DTO列表
     *
     * @return 部门DTO列表
     */
    @Override
    public List<DepartmentDto> listDto() {
        return this.list().stream()
                .map(converters::toDepartmentDto)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取部门DTO
     *
     * @param id 部门ID
     * @return 部门DTO对象
     */
    @Override
    public DepartmentDto getDtoById(Long id) {
        Department department = this.getById(id);
        return department != null ? converters.toDepartmentDto(department) : null;
    }
    //#endregion

    //#region 部门查询接口
    // ===================================
    // 部门查询接口
    // ===================================
    
    /**
     * 高级查询部门（默认分页参数）
     * @return 部门列表的分页结果
     */
    @Override
    public PagedResult<DepartmentDto> advancedSearchDepartments() {
        return advancedSearchDepartments(null, null, 0, -1);
    }

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
    @Override
    public PagedResult<DepartmentDto> advancedSearchDepartments(
        String departmentName,
        Integer status,
        int pageIndex,
        int pageSize) {
        
        QueryWrapper<Department> queryWrapper = new QueryWrapper<>();
        
        // 部门名称模糊查询
        if (departmentName != null && !departmentName.isEmpty()) {
            queryWrapper.like("department_name", departmentName);
        }
        
        // 获取总数
        long totalCount = this.count(queryWrapper);
        
        // 分页处理
        List<Department> departmentEntities;
        if (pageSize == -1) {
            departmentEntities = this.list(queryWrapper);
        } else {
            departmentEntities = this.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageIndex + 1, pageSize), 
                queryWrapper).getRecords();
        }
        
        // 转换为DTO
        List<DepartmentDto> departmentDtos = departmentEntities.stream()
                .map(converters::toDepartmentDto)
                .collect(Collectors.toList());
        
        // 构建分页结果
        PagedResult<DepartmentDto> pagedResult = new PagedResult<>();
        pagedResult.setItems(departmentDtos);
        pagedResult.setTotalCount(totalCount);
        pagedResult.setPageIndex(pageIndex);
        
        if (pageSize == -1) {
            pagedResult.setPageSize((int) totalCount);
        } else {
            pagedResult.setPageSize(pageSize);
        }
        
        return pagedResult;
    }
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
    @Override
    public DepartmentDto convertToDto(Department department) {
        return converters.toDepartmentDto(department);
    }
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
    @Override
    public Department getByDepartmentName(String departmentName) {
        return this.getOne(new QueryWrapper<Department>().eq("department_name", departmentName));
    }
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
    @Override
    public boolean hasUserDepartments(Long departmentId) {
        return userDepartmentService.count(new QueryWrapper<com.tonghui.erp.Data.Entity.UserDepartment>()
                .eq("department_id", departmentId)) > 0;
    }
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
    @Override
    public DepartmentDto getDepartmentDetails(Long departmentId) {
        Department department = this.getById(departmentId);
        return department != null ? converters.toDepartmentDto(department) : null;
    }
    //#endregion
}