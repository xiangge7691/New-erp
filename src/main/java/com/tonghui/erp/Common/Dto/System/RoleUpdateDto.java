package com.tonghui.erp.Common.Dto.System;

import lombok.Data;
import java.util.List;

/**
 * 角色更新信息传输对象
 * <p>
 * 用于封装更新角色时所需的信息，包括角色基本信息和关联的权限ID列表
 * </p>
 */
@Data
public class RoleUpdateDto {
    
    //#region 角色基本信息字段
    // ===================================
    // 角色基本信息字段
    // ===================================
    
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
    
    //#endregion
    
    //#region 权限关联字段
    // ===================================
    // 权限关联字段
    // ===================================
    
    /**
     * 权限ID列表
     * <p>角色关联的权限ID集合</p>
     */
    private List<Long> permissionIds;
    
    //#endregion
}
