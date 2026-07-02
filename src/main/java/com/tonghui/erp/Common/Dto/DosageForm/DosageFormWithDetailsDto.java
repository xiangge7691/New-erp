package com.tonghui.erp.Common.Dto.DosageForm;

import com.tonghui.erp.Data.Entity.DosageForm;
import com.tonghui.erp.Data.Entity.Preparation;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DosageFormWithDetailsDto extends DosageForm {
    private List<Preparation> preparations;
}
