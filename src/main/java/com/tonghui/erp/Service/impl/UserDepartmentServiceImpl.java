package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.UserDepartment;
import com.tonghui.erp.Service.UserDepartmentService;
import com.tonghui.erp.Data.mapper.UserDepartmentMapper;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.UserDepartmentDto;
import com.tonghui.erp.Common.Mapper.Converters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户部门服务实现类
 * <p>
 * 实现UserDepartmentService接口，提供用户部门关联相关的业务逻辑处理，包括用户部门关联的查询、管理等功能的具体实现
 * </p>
 * 
 * @author 87954
 * @description 针对表【user_department(用户-部门关联表)】的数据库操作Service实现
 * @createDate 2025-08-27 10:08:57
 */
@Service
public class UserDepartmentServiceImpl extends ServiceImpl<UserDepartmentMapper, UserDepartment>
    implements UserDepartmentService{

    @Autowired
    private Converters converters;

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
    @Override
    public List<UserDepartment> getByUserId(Long userId) {
        return this.list(new QueryWrapper<UserDepartment>().eq("user_id", userId));
    }

    /**
     * 根据部门ID获取所有用户关联
     *
     * @param departmentId 部门ID
     * @return 用户部门关联列表
     */
    @Override
    public List<UserDepartment> getByDepartmentId(Long departmentId) {
        return this.list(new QueryWrapper<UserDepartment>().eq("department_id", departmentId));
    }
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
    @Override
    public UserDepartment getPrimaryDepartment(Long userId) {
        return this.getOne(new QueryWrapper<UserDepartment>()
                .eq("user_id", userId)
                .eq("is_primary", 1));
    }
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
    @Override
    public PagedResult<UserDepartment> getPaged(int pageIndex, int pageSize) {
        Page<UserDepartment> page = new Page<>(pageIndex + 1, pageSize);
        Page<UserDepartment> result = this.page(page, new QueryWrapper<>());
        
        PagedResult<UserDepartment> pagedResult = new PagedResult<>();
        pagedResult.setItems(result.getRecords());
        pagedResult.setTotalCount(result.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize(pageSize);
        
        return pagedResult;
    }

    /**
     * 根据查询条件获取用户部门关联（分页）
     *
     * @param userId           用户ID
     * @param departmentId     部门ID
     * @param isPrimary        是否主部门
     * @param createdStartTime 创建时间起始
     * @param createdEndTime   创建时间结束
     * @param pageIndex        页码
     * @param pageSize         每页数量
     * @return 分页结果
     */
    @Override
    public PagedResult<UserDepartment> searchUserDepartments(
            Long userId,
            Long departmentId,
            Integer isPrimary,
            String createdStartTime,
            String createdEndTime,
            int pageIndex,
            int pageSize) {
        
        QueryWrapper<UserDepartment> queryWrapper = new QueryWrapper<>();
        
        // 添加查询条件
        if (userId != null) {
            queryWrapper.eq("user_id", userId);
        }
        
        if (departmentId != null) {
            queryWrapper.eq("department_id", departmentId);
        }
        
        if (isPrimary != null) {
            queryWrapper.eq("is_primary", isPrimary);
        }
        
        if (createdStartTime != null && !createdStartTime.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startTime = LocalDateTime.parse(createdStartTime, formatter);
            queryWrapper.ge("created_at", startTime);
        }
        
        if (createdEndTime != null && !createdEndTime.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime endTime = LocalDateTime.parse(createdEndTime, formatter);
            queryWrapper.le("created_at", endTime);
        }
        
        Page<UserDepartment> page = new Page<>(pageIndex + 1, pageSize);
        Page<UserDepartment> result = this.page(page, queryWrapper);
        
        PagedResult<UserDepartment> pagedResult = new PagedResult<>();
        pagedResult.setItems(result.getRecords());
        pagedResult.setTotalCount(result.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize(pageSize);
        
        return pagedResult;
    }
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
    @Override
    public PagedResult<UserDepartmentDto> getPagedDto(int pageIndex, int pageSize) {
        PagedResult<UserDepartment> pagedResult = getPaged(pageIndex, pageSize);
        
        List<UserDepartmentDto> dtoItems = pagedResult.getItems().stream()
                .map(converters::toUserDepartmentDto)
                .collect(Collectors.toList());
        
        PagedResult<UserDepartmentDto> dtoPagedResult = new PagedResult<>();
        dtoPagedResult.setItems(dtoItems);
        dtoPagedResult.setTotalCount(pagedResult.getTotalCount());
        dtoPagedResult.setPageIndex(pagedResult.getPageIndex());
        dtoPagedResult.setPageSize(pagedResult.getPageSize());
        
        return dtoPagedResult;
    }
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
    @Override
    public List<UserDepartmentDto> getDtosByUserId(Long userId) {
        List<UserDepartment> userDepartments = getByUserId(userId);
        
        return userDepartments.stream()
                .map(converters::toUserDepartmentDto)
                .collect(Collectors.toList());
    }

    /**
     * 根据部门ID获取所有用户关联DTO
     *
     * @param departmentId 部门ID
     * @return 用户部门关联DTO列表
     */
    @Override
    public List<UserDepartmentDto> getDtosByDepartmentId(Long departmentId) {
        List<UserDepartment> userDepartments = getByDepartmentId(departmentId);
        
        return userDepartments.stream()
                .map(converters::toUserDepartmentDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的主部门DTO
     *
     * @param userId 用户ID
     * @return 主部门关联DTO
     */
    @Override
    public UserDepartmentDto getPrimaryDepartmentDto(Long userId) {
        UserDepartment primaryDepartment = getPrimaryDepartment(userId);
        
        if (primaryDepartment == null) {
            return null;
        }
        
        return converters.toUserDepartmentDto(primaryDepartment);
    }
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
    public UserDepartmentDto convertToDto(UserDepartment entity) {
        return converters.toUserDepartmentDto(entity);
    }
    //#endregion
}




