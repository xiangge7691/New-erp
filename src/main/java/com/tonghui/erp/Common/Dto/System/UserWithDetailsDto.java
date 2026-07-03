package com.tonghui.erp.Common.Dto.System;

import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.Entity.UserRole;
import com.tonghui.erp.Data.Entity.UserDepartment;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户包含角色、部门和人事档案的扩展数据传输对象
 * <p>
 * 在用户基础上扩展了角色、部门和人事档案列表，用于展示完整的用户详情
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserWithDetailsDto extends User {

    /**
     * 该用户的角色列表
     */
    private List<UserRole> roles;

    /**
     * 该用户的部门列表
     */
    private List<UserDepartment> departments;

    /**
     * 该用户的人事档案列表
     */
    private List<PersonnelFile> personnelFiles;
}
