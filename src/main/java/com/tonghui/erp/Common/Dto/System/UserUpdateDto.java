package com.tonghui.erp.Common.Dto.System;

import lombok.Data;
import java.util.List;

/**
 * 用户更新信息传输对象
 * <p>
 * 用于封装更新用户时所需的信息，包括用户基本信息、角色ID列表和部门ID列表
 * </p>
 */
@Data
public class UserUpdateDto {
    
    //#region 用户基本信息字段
    // ===================================
    // 用户基本信息字段
    // ===================================
    
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
     * 密码
     * <p>用户的登录密码</p>
     */
    private String password;
    
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
     * <p>用户的状态信息</p>
     */
    private Object status;
    
    /**
     * 备注
     * <p>用户的备注信息</p>
     */
    private String notes;
    
    //#endregion
    
    //#region 关联关系字段
    // ===================================
    // 关联关系字段
    // ===================================
    
    /**
     * 角色ID列表
     * <p>用户关联的角色ID集合</p>
     */
    private List<Long> roleIds;
    
    /**
     * 部门ID列表
     * <p>用户关联的部门ID集合</p>
     */
    private List<Long> departmentIds;
    
    //#endregion
}
