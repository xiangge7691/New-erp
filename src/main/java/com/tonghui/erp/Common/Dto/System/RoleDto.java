package com.tonghui.erp.Common.Dto.System;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * 角色信息DTO
 * <p>
 * 用于封装角色信息的数据传输对象，包含角色的基本信息、状态以及关联的用户和权限列表
 * </p>
 */
@Data
public class RoleDto {
    
    //#region 角色基本信息字段
    // ===================================
    // 角色基本信息字段
    // ===================================
    
    /**
     * 角色ID
     * <p>角色的唯一标识符</p>
     */
    private Long roleId;
    
    /**
     * 角色名称
     * <p>角色的名称信息</p>
     */
    private String roleName;
    
    /**
     * 角色描述
     * <p>角色的详细描述信息</p>
     */
    private String roleDesc;
    
    /**
     * 角色状态
     * <p>角色的状态，如启用(1)、禁用(0)等</p>
     */
    private Integer status;

    /**
     * 创建时间
     * <p>角色记录的创建时间</p>
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     * <p>角色记录的最后更新时间</p>
     */
    private LocalDateTime updatedTime;
    
    //#endregion
    
    //#region 关联关系字段
    // ===================================
    // 关联关系字段
    // ===================================
    
    /**
     * 关联用户列表
     * <p>拥有当前角色的用户集合</p>
     */
    private List<UserDto> users = new ArrayList<>();
    
    /**
     * 关联权限列表
     * <p>当前角色拥有的权限集合</p>
     */
    private List<PermissionDto> permissions = new ArrayList<>();
    
    //#endregion
}
