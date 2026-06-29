package com.tonghui.erp.Common.Dto.System;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 部门信息DTO
 * <p>
 * 用于封装部门信息的数据传输对象，包含部门的基本信息和状态
 * </p>
 */
@Data
public class DepartmentDto {
    
    //#region 部门基本信息字段
    // ===================================
    // 部门基本信息字段
    // ===================================
    
    /**
     * 部门ID
     * <p>部门的唯一标识符</p>
     */
    private Long id;
    
    /**
     * 部门名称
     * <p>部门的名称信息</p>
     */
    private String departmentName;

    /**
     * 创建时间
     * <p>部门记录的创建时间</p>
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     * <p>部门记录的最后更新时间</p>
     */
    private LocalDateTime updatedAt;
    
    //#endregion
}