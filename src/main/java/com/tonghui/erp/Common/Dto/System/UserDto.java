package com.tonghui.erp.Common.Dto.System;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息传输对象
 * <p>
 * 用于封装用户信息的数据传输对象，包含用户的基本信息、状态以及关联的部门和角色列表
 * </p>
 */
@Data
public class UserDto {
    
    //#region 用户基本信息字段
    // ===================================
    // 用户基本信息字段
    // ===================================
    
    /**
     * 用户ID
     * <p>用户的唯一标识符</p>
     */
    private Long id;
    
    /**
     * 用户名
     * <p>用户的登录用户名</p>
     */
    private String userName;
    
    /**
     * 姓名
     * <p>用户的真实姓名</p>
     */
    private String name;
    
    /**
     * 电话
     * <p>用户的联系电话</p>
     */
    private String phone;
    
    /**
     * 性别
     * <p>用户的性别信息</p>
     */
    private Object gender;
    
    /**
     * 状态
     * <p>用户的状态信息，如启用(1)、禁用(0)等</p>
     */
    private Integer status;
    
    /**
     * 备注
     * <p>用户的备注信息</p>
     */
    private String notes;
    
    /**
     * 创建时间
     * <p>用户记录的创建时间</p>
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     * <p>用户记录的最后更新时间</p>
     */
    private LocalDateTime updatedTime;
    
    //#endregion
    
    //#region 关联关系字段
    // ===================================
    // 关联关系字段
    // ===================================
    
    /**
     * 关联部门列表
     * <p>用户关联的部门集合</p>
     */
    private List<DepartmentDto> departments;
    
    /**
     * 主部门
     * <p>用户的主部门信息</p>
     */
    private DepartmentDto primaryDepartment;
    
    /**
     * 关联角色列表
     * <p>用户关联的角色集合</p>
     */
    private List<RoleDto> roles;
    
    //#endregion
}