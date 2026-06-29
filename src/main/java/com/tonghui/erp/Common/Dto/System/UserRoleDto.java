package com.tonghui.erp.Common.Dto.System;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户角色关联信息传输对象
 * <p>
 * 用于封装用户与角色关联关系的数据传输对象，包含关联关系的基本信息
 * </p>
 */
@Data
public class UserRoleDto {
    
    //#region 关联关系基本信息字段
    // ===================================
    // 关联关系基本信息字段
    // ===================================
    
    /**
     * 关联ID
     * <p>用户与角色关联关系的唯一标识符</p>
     */
    private Long id;
    
    /**
     * 用户ID
     * <p>关联的用户ID</p>
     */
    private Long userId;
    
    /**
     * 角色ID
     * <p>关联的角色ID</p>
     */
    private Long roleId;
    
    /**
     * 创建时间
     * <p>关联关系的创建时间</p>
     */
    private LocalDateTime createdAt;
    
    //#endregion
}
