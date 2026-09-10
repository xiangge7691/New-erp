package com.tonghui.erp.Common.Dto.System;

import com.tonghui.erp.Data.Entity.Department;
import com.tonghui.erp.Data.Entity.Position;
import com.tonghui.erp.Data.Entity.UserDepartment;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DepartmentWithDetailsDto extends Department {
    private List<Position> positions;
    private List<UserDepartment> userDepartments;
}
