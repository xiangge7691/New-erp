package com.tonghui.erp.Common.Dto.System;

import com.tonghui.erp.Data.Entity.User;
import com.tonghui.erp.Data.Entity.UserRole;
import com.tonghui.erp.Data.Entity.UserDepartment;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserWithDetailsDto extends User {
    private List<UserRole> roles;
    private List<UserDepartment> departments;
    private List<PersonnelFile> personnelFiles;
}
