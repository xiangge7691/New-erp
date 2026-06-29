package com.tonghui.erp.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tonghui.erp.Data.Entity.UserRole;
import com.tonghui.erp.Service.UserRoleService;
import com.tonghui.erp.Data.mapper.UserRoleMapper;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Common.Dto.System.UserRoleDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户角色服务实现类
 * <p>
 * 实现UserRoleService接口，提供用户角色关联相关的业务逻辑处理，包括用户角色关联的查询、管理等功能的具体实现
 * </p>
 * 
 * @author 87954
 * @description 针对表【user_role(用户-角色关联表)】的数据库操作Service实现
 * @createDate 2025-08-27 10:08:57
 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole>
    implements UserRoleService{

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
    @Override
    public List<UserRole> getByUserId(Long userId) {
        return this.list(new QueryWrapper<UserRole>().eq("user_id", userId));
    }

    /**
     * 根据角色ID获取所有用户关联
     *
     * @param roleId 角色ID
     * @return 用户角色关联列表
     */
    @Override
    public List<UserRole> getByRoleId(Long roleId) {
        return this.list(new QueryWrapper<UserRole>().eq("role_id", roleId));
    }
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
    @Override
    public List<UserRoleDto> getDtosByUserId(Long userId) {
        List<UserRole> userRoles = getByUserId(userId);
        return userRoles.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 根据角色ID获取所有用户关联DTO
     *
     * @param roleId 角色ID
     * @return 用户角色关联DTO列表
     */
    @Override
    public List<UserRoleDto> getDtosByRoleId(Long roleId) {
        List<UserRole> userRoles = getByRoleId(roleId);
        return userRoles.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
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
    @Override
    public UserRoleDto convertToDto(UserRole entity) {
        if (entity == null) return null;

        UserRoleDto dto = new UserRoleDto();
        dto.setId(entity.getRoleId());
        dto.setUserId(entity.getUserId());
        dto.setRoleId(entity.getRoleId());
        dto.setCreatedAt(entity.getCreatedAt());
        
        return dto;
    }
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
    @Override
    public PagedResult<UserRoleDto> advancedSearchUserRoles(
            Long userId,
            Long roleId,
            String createdStartTime,
            String createdEndTime,
            int pageIndex,
            int pageSize) {
        
        QueryWrapper<UserRole> queryWrapper = new QueryWrapper<>();

        // 用户ID筛选
        if (userId != null) {
            queryWrapper.eq("user_id", userId);
        }

        // 角色ID筛选
        if (roleId != null) {
            queryWrapper.eq("role_id", roleId);
        }

        // 创建时间起始筛选
        if (createdStartTime != null && !createdStartTime.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startTime = LocalDateTime.parse(createdStartTime, formatter);
            queryWrapper.ge("created_at", startTime);
        }

        // 创建时间结束筛选
        if (createdEndTime != null && !createdEndTime.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime endTime = LocalDateTime.parse(createdEndTime, formatter);
            queryWrapper.le("created_at", endTime);
        }

        List<UserRole> userRoleEntities;
        long totalCount;
        
        // 如果pageSize为-1，返回所有结果，不分页
        if (pageSize == -1) {
            userRoleEntities = this.list(queryWrapper.orderByAsc("id"));
            totalCount = userRoleEntities.size();
        } else {
            Page<UserRole> page = new Page<>(pageIndex + 1, pageSize);
            Page<UserRole> result = this.page(page, queryWrapper.orderByAsc("id"));
            userRoleEntities = result.getRecords();
            totalCount = result.getTotal();
        }

        List<UserRoleDto> userRoleDtos = userRoleEntities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        PagedResult<UserRoleDto> pagedResult = new PagedResult<>();
        pagedResult.setItems(userRoleDtos);
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
    @Override
    public PagedResult<UserRole> searchUserRoles(
            Long userId,
            Long roleId,
            String createdStartTime,
            String createdEndTime,
            int pageIndex,
            int pageSize) {
        
        QueryWrapper<UserRole> queryWrapper = new QueryWrapper<>();

        // 添加查询条件
        if (userId != null) {
            queryWrapper.eq("user_id", userId);
        }

        if (roleId != null) {
            queryWrapper.eq("role_id", roleId);
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

        Page<UserRole> page = new Page<>(pageIndex + 1, pageSize);
        Page<UserRole> result = this.page(page, queryWrapper.orderByAsc("id"));

        PagedResult<UserRole> pagedResult = new PagedResult<>();
        pagedResult.setItems(result.getRecords());
        pagedResult.setTotalCount(result.getTotal());
        pagedResult.setPageIndex(pageIndex);
        pagedResult.setPageSize(pageSize);

        return pagedResult;
    }

    /**
     * 分页获取所有用户角色关联DTO
     *
     * @param pageIndex 页码
     * @param pageSize  每页数量
     * @return 分页结果
     */
    @Override
    public PagedResult<UserRoleDto> getPagedDto(int pageIndex, int pageSize) {
        return advancedSearchUserRoles(null, null, null, null, pageIndex, pageSize);
    }
    //#endregion
}




