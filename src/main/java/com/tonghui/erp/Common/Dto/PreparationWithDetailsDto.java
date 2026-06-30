package com.tonghui.erp.Common.Dto;

import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.PreparationDocument;
import com.tonghui.erp.Data.Entity.PreparationFormula;
import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PreparationWithDetailsDto extends Preparation {
    private List<PreparationFormula> formulas;
    private List<PreparationDocument> documents;
    private List<PreparationProcessTemplate> processTemplates;
}
