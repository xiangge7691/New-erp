package com.tonghui.erp.Common.Dto.System;

import com.tonghui.erp.Data.Entity.Role;
import com.tonghui.erp.Data.Entity.RolePerm;
import com.tonghui.erp.Data.Entity.UserRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleWithDetailsDto extends Role {
    private List<RolePerm> permissions;
    private List<UserRole> userRoles;
}
