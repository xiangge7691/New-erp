package com.tonghui.erp.Common.Dto;

import com.tonghui.erp.Data.Entity.PreparationProcessTemplate;
import com.tonghui.erp.Data.Entity.ProcessType;
import com.tonghui.erp.Data.Entity.ProductionProcessRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessTypeWithDetailsDto extends ProcessType {
    private List<ProductionProcessRecord> records;
    private List<PreparationProcessTemplate> templates;
}
