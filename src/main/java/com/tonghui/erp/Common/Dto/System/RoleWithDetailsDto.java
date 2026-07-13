package com.tonghui.erp.Common.Dto.System;

import com.tonghui.erp.Data.Entity.Role;
import com.tonghui.erp.Data.Entity.UserRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 角色详细信息DTO（包含权限和用户关联）
 * <p>
 * 继承自Role实体，扩展了权限列表和用户角色关联信息
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleWithDetailsDto extends Role {
    
    /**
     * 关联权限列表
     * <p>当前角色拥有的权限详情集合</p>
     */
    private List<PermissionDto> permissions;
    
    /**
     * 关联用户角色列表
     * <p>当前角色关联的用户角色关系集合</p>
     */
    private List<UserRole> userRoles;
}
