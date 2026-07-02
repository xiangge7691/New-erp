package com.tonghui.erp.Common.Dto.System;

import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.Position;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PositionWithDetailsDto extends Position {
    private List<PersonnelFile> personnelFiles;
}
