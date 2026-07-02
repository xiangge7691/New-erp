package com.tonghui.erp.Common.Dto;

import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.PersonnelCertificate;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PersonnelFileWithDetailsDto extends PersonnelFile {
    private List<PersonnelCertificate> certificates;
}
