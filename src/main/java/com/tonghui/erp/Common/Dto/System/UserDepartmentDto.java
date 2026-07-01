package com.tonghui.erp.Common.Dto.System;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户部门关联信息传输对象
 * <p>
 * 用于封装用户与部门关联关系的数据传输对象，包含关联关系的基本信息和状态
 * </p>
 */
@Data
public class UserDepartmentDto {
    
    //#region 关联关系基本信息字段
    // ===================================
    // 关联关系基本信息字段
    // ===================================
    
    /**
     * 关联ID
     * <p>用户与部门关联关系的唯一标识符</p>
     */
    private Long id;
    
    /**
     * 用户ID
     * <p>关联的用户ID</p>
     */
    private Long userId;
    
    /**
     * 部门ID
     * <p>关联的部门ID</p>
     */
    private Long departmentId;
    
    /**
     * 是否为主部门
     * <p>标识该部门是否为用户的主部门，1-是，0-否</p>
     */
    private Integer isPrimary;
    
    /**
     * 创建时间
     * <p>关联关系的创建时间</p>
     */
    private LocalDateTime createdTime;
    
    //#endregion
}
