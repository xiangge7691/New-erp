package com.tonghui.erp.Common.Dto.System;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * 权限信息DTO
 * <p>
 * 用于封装权限信息的数据传输对象，包含权限的基本信息、状态以及关联的角色和子权限
 * </p>
 */
@Data
public class PermissionDto {
    
    //#region 权限基本信息字段
    // ===================================
    // 权限基本信息字段
    // ===================================
    
    /**
     * 权限ID
     * <p>权限的唯一标识符</p>
     */
    private Long id;
    
    /**
     * 权限键
     * <p>权限的唯一键值，用于程序中权限验证</p>
     */
    private String permKey;
    
    /**
     * 权限名称
     * <p>权限的显示名称</p>
     */
    private String permName;
    
    /**
     * 权限类型
     * <p>权限的类型，如菜单、按钮等</p>
     */
    private String permType;
    
    /**
     * 父级权限ID
     * <p>父级权限的ID，用于构建权限树结构</p>
     */
    private Long parentId;
    
    /**
     * 显示顺序
     * <p>权限在界面中的显示顺序</p>
     */
    private Integer displayOrder;
    
    /**
     * 权限状态
     * <p>权限的状态，如启用(1)、禁用(0)等</p>
     */
    private Integer status; // 修改字段名为status，与前端传参一致
    
    /**
     * 创建时间
     * <p>权限记录的创建时间</p>
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     * <p>权限记录的最后更新时间</p>
     */
    private LocalDateTime updatedTime;
    
    //#endregion
    
    //#region 关联关系字段
    // ===================================
    // 关联关系字段
    // ===================================
    
    /**
     * 子权限列表
     * <p>当前权限的子权限集合，用于构建权限树</p>
     */
    private List<PermissionDto> children = new ArrayList<>();
    
    /**
     * 关联角色列表
     * <p>拥有当前权限的角色集合</p>
     */
    private List<RoleDto> roles = new ArrayList<>();
    
    //#endregion
}
